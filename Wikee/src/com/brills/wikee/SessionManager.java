package com.brills.wikee;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

// A session is a tree:
// Node: a query (key), along with its first visit time
// Edge: a -> b if the first visit to b is via a.
// Every new search creates a new session.
// Clicking Wikipedia links in an article creates a node in a session, if the link refers to an
// article that has not been visited.
// SessionManager maintains a "cursor", which always points to the current visiting page. When
// clicking an link referring to a visited page, or push "back" button, or choose an visited page in
// the history list, the cursor will be moved to that page.

class SessionManager {
  // Node of the session tree
  static public class SessionNode {
    final String key_;
    final private Date first_visit_date_;
    final SessionNode parent_;
    final ArrayList<SessionNode> children_;
    final long node_id_;
    final long session_id_;

    public SessionNode(String key, Date date, SessionNode parent, long node_id, long session_id) {
      key_ = key;
      first_visit_date_ = date;
      parent_ = parent;
      children_ = new ArrayList<SessionNode>();
      node_id_ = node_id;
      session_id_ = session_id;
    }

    public ArrayList<SessionNode> GetChildrenList() {
      return children_;
    }

    public Date GetDate() {
      return first_visit_date_;
    }

    public long GetId() {
      return node_id_;
    }

    public String GetKey() {
      return key_;
    }

    public long GetSessionId() {
      return session_id_;
    }
  }

  // SQLite helper
  static private class SessionDBOpenHelper extends SQLiteOpenHelper {
    // Schema:
    // Node that node_id is the only primary key.
    // Table "node": (session_id, *node_id, key, first_visit_date)
    // Table "edge": (#session_id, #parent_id, #child_id)
    // Table "session": (*session_id, last_update_date)
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "session";
    private static final String NODE_TABLE_NAME = "node";
    private static final String SESSION_TABLE_NAME = "session";
    private static final String EDGE_TABLE_NAME = "edge";
    private final SQLiteStatement NEXT_NODE_ID_STMT = getReadableDatabase().compileStatement(
        "SELECT seq FROM SQLITE_SEQUENCE WHERE name='" + NODE_TABLE_NAME + "';");
    private final SQLiteStatement NEXT_SESSION_ID_STMT = getReadableDatabase().compileStatement(
        "SELECT seq FROM SQLITE_SEQUENCE WHERE name='" + SESSION_TABLE_NAME + "';");

    SessionDBOpenHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + SESSION_TABLE_NAME + "("
          + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + "date NOT NULL TEXT,"
          + "root_node_id NULL INTEGER," // Note this field is null initially to break the cyclic
                                         // foreign key references
          + "FOREIGN KEY(root_node_id) REFERENCES " + NODE_TABLE_NAME + "(id)" + ");");
      db.execSQL("CREATE TABLE " + NODE_TABLE_NAME + "("
          + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," + "session_id INTEGER," + "key TEXT,"
          + "date INTEGER," + "FOREIGN KEY(session_id) REFERENCES " + SESSION_TABLE_NAME + "(id)"
          + ");");
      db.execSQL("CREATE TABLE " + EDGE_TABLE_NAME + "(" + "session_id INTEGER NOT NULL,"
          + "parent_id INTEGER," + "child_id INTEGER NOT NULL,"
          + "FOREIGN KEY(session_id) REFERENCES " + SESSION_TABLE_NAME + "(id),"
          + "FOREIGN KEY(parent_id) REFERENCES " + NODE_TABLE_NAME + "(id),"
          + "FOREIGH KEY(child_id) REFERENCES " + NODE_TABLE_NAME + "(id)" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {}

    public long GetNextNodeId() {
      long current_id;
      try {
        current_id = NEXT_NODE_ID_STMT.simpleQueryForLong();
      } catch (SQLiteDoneException e) {
        current_id = 0;
      }
      return current_id + 1;
    }

    public long GetNextSessionId() {
      long current_id;
      try {
        current_id = NEXT_SESSION_ID_STMT.simpleQueryForLong();
      } catch (SQLiteDoneException e) {
        current_id = 0;
      }
      return current_id + 1;
    }

    public void NewSession(SessionNode root) {
      SQLiteDatabase db = getWritableDatabase();
      long next_session_id = GetNextSessionId();
      db.beginTransaction();
      db.insert(SESSION_TABLE_NAME, "root_node_id", new ContentValues());

      ContentValues values = new ContentValues();
      values.put("session_id", next_session_id);
      values.put("key", root.GetKey());
      values.put("date", DateToMilliSecond(root.GetDate()));
      db.insert(NODE_TABLE_NAME, null, values);

      values.clear();
      values.put("root_node_id", root.GetId());
      db.update(SESSION_TABLE_NAME, values, "session_id=" + next_session_id, null);
      db.endTransaction();
    }

    public void NewNode(SessionNode parent, SessionNode new_node) {
      SQLiteDatabase db = getWritableDatabase();
       db.beginTransaction();
       ContentValues values = new ContentValues();
       values.put("session_id", new_node.GetSessionId());
       values.put("key", new_node.GetKey());
       values.put("date", DateToMilliSecond(new_node.GetDate()));
       db.insert(NODE_TABLE_NAME, null, values);

       values.clear();
       values.put("parent_id", parent.GetId());
       values.put("child_id", new_node.GetId());
       values.put("session_id", new_node.GetSessionId());
       db.insert(EDGE_TABLE_NAME, null, values);
       db.endTransaction();
    }
  }

  private static long DateToMilliSecond(Date date) {
    return date.getTime();
  }

  private static Date MilliSecondToDate(long ms) {
    return new Date(ms);
  }

  private final SessionNode root_;
  private SessionNode cursor_;
  private final HashMap<String, SessionNode> hash_;
  private final long session_id_;
  private final SessionDBOpenHelper db_helper_;

  // Start a session with a query
  public SessionManager(String key, Context context) {
    db_helper_ = new SessionDBOpenHelper(context);
    session_id_ = db_helper_.GetNextSessionId();
    root_ =
        new SessionNode(key, new Date(System.currentTimeMillis()), null,
       db_helper_.GetNextNodeId(), session_id_);
    db_helper_.NewSession(root_);
    cursor_ = root_;
    hash_ = new HashMap<String, SessionNode>();
    hash_.put(key, root_);
  }

  // Load a session with a session_id;
  // public SessionManager(int session_id, Context context) {}

  public void NewQueryInSession(String key) {
    SessionNode visited = hash_.get(key);
    if (visited == null) {
      SessionNode new_node =
          new SessionNode(key, new Date(System.currentTimeMillis()), cursor_,
              db_helper_.GetNextNodeId(), session_id_);
      cursor_.GetChildrenList().add(new_node);
      SessionNode parent = cursor_;
      cursor_ = new_node;
      db_helper_.NewNode(parent, cursor_);
    } else {
      cursor_ = visited;
    }
  }
}
