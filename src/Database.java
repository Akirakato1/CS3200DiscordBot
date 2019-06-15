import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class Database {

  private final String userName;
  private final String password;
  private final String serverName;
  private final int portNumber;
  private final String dbName;
  private Connection conn;

  Database(String un, String pw, String server, int port, String db) throws SQLException {
    this.userName = un;
    this.password = pw;
    this.serverName = server;
    this.portNumber = port;
    this.dbName = db;
    try {
      this.conn = this.getConnection();
      System.out.println("Connection Established");
    }
    catch (SQLException e) {
      System.out.println("ERROR: Could not connect to the database");
      e.printStackTrace();
      return;
    }
  }

  public void createInvite(String sender, String receiver, String channel_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "INSERT IGNORE INTO Invite (sender,receiver,instance_id)" + " Values ("
          + sender + ", " + receiver + ", " + channel_id + ")";
      statement.executeUpdate(command);
      System.out.println("Created invite from to " + channel_id);
    }
    catch (SQLException e) {
      System.out.println("could not create invite for " + channel_id);
      e.printStackTrace();
    }
  }

  public void createInstance(String privacy, String game_id, int freespot, long channel_id,
      String name) {
    try {
      Statement statement = this.conn.createStatement();
      String visibility;
      if (privacy.equals("private")) {
        visibility = "false";
      }
      else {
        visibility = "true";
      }

      String command = "INSERT INTO Instance (instance_id,free_spots,game_id,public)" + " Values ("
          + channel_id + ", " + freespot + ", " + game_id + ", " + visibility + ")";
      statement.executeUpdate(command);
      System.out.println("Created instance in database: " + name);
    }
    catch (SQLException e) {
      System.out.println("could not create instance in database: " + name);
      e.printStackTrace();
    }
  }

  public void deleteInstance(long channel_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Delete from Instance where instance_id="+channel_id;
      statement.executeUpdate(command);
      System.out.println("Deleted instance "+channel_id);
    }
    catch (SQLException e) {
      System.out.println("Error when deleting instance "+ channel_id);
      e.printStackTrace();
    }
  }
  
  public String getIDbyNameGameType(String gametype) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select game_id from GameType where name='" + gametype+"'";
      ResultSet result = statement.executeQuery(command);
      String gameid = "invalid gametype";
      while (result.next()) {
        gameid = result.getString("game_id");
      }
      System.out.println("Gametype: " + gametype + " has ID of: " + gameid);
      return gameid;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving gametype_id");
      e.printStackTrace();
      return "no id";
    }
  }
  
  public ArrayList<Long> getInstanceIDbyName(String name){
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select instance_id from Instance where instance_name='" + name+"'";
      ResultSet result = statement.executeQuery(command);
      ArrayList<Long> ids=new ArrayList<Long>();
      while (result.next()) {
        ids.add(result.getLong("instance_id"));
      }
      if(ids.size()==0) {
        return null;
        }
      return ids;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving gametype_id");
      e.printStackTrace();
      return null;
    }
  }

  public void updatePlayers(JDA api) throws SQLException {
    ArrayList<String> member_id = this.getFieldFromTable("Player", "player_id");

    List<Guild> guilds = api.getGuilds();
    for (Guild g : guilds) {
      for (Member m : g.getMembers()) {
        if (!member_id.contains(m.getUser().getId())) {
          this.initPlayer(m.getUser());
        }
      }
    }
  }

  public void initPlayer(User user) throws SQLException {
    if (!user.isBot()) {
      try {
        String command = "INSERT IGNORE INTO Player (player_id,nickname,exp,enable_notification,last_played)"
            + " Values ('" + user.getId() + "','" + user.getName() + "', 0, true,null)";

        Statement statement = this.conn.createStatement();
        statement.executeUpdate(command);
        System.out.println("User initialised on database: " + user.getName());
      }
      catch (SQLException e) {
        System.out.println("could not add user " + user.getName());
        e.printStackTrace();
      }
    }
  }

  public Connection getConnection() throws SQLException {
    Connection conn = null;
    Properties connectionProps = new Properties();
    connectionProps.put("user", this.userName);
    connectionProps.put("password", this.password);

    conn = DriverManager.getConnection(
        "jdbc:mysql://" + this.serverName + ":" + this.portNumber + "/" + this.dbName,
        connectionProps);

    return conn;
  }

  public boolean executeUpdate(Connection conn, String command) throws SQLException {
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      stmt.executeUpdate(command); // This will throw a SQLException if it fails
      return true;
    }
    finally {

      // This will run whether we throw an exception or not
      if (stmt != null) {
        stmt.close();
      }
    }
  }

  public ResultSet getResultSet(String command) throws SQLException {
    PreparedStatement statement = this.conn.prepareStatement(command);
    ResultSet result = statement.executeQuery();
    return result;
  }

  public ArrayList<String> getFieldFromTable(String tablename, String field) {
    try {
      String command = "Select " + field + " from " + tablename;
      ResultSet result = this.getResultSet(command);
      ArrayList<String> array = new ArrayList<String>();
      while (result.next()) {
        array.add(result.getString(field));
      }
      return array;
    }
    catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve " + field + " from " + tablename);
      e.printStackTrace();
      return null;
    }
  }

  public void closeConnection() throws SQLException {
    this.conn.close();
  }
}