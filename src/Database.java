import java.sql.CallableStatement;
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

  public void joinInstance(long player_id, long channel_id) {
    try {
      CallableStatement cs=this.conn.prepareCall("{Call joinInstance("+player_id+", "+channel_id+")}");
      cs.executeUpdate();
      System.out.println("player joined instance");
    }
    catch (SQLException e) {
      System.out.println("player could not join instance");
      e.printStackTrace();
    }
  }
  
  public Integer getTeamIDbyName(long instance_id, String name) {
    String pcondition="instance_id="+instance_id+" and team_name='"+name+"'";
    String result=this.getFieldWithConditionTable("Team", "team_id", pcondition);
    if(result==null) {
      return null;
    }
    return Integer.parseInt(result);
  }
 
  public void createTuple(String table,String fields, String values) {
    try {
      Statement statement = this.conn.createStatement();
      String command="insert into "+table+" ("+fields+") values ("+values+")";
      statement.executeUpdate(command);
      System.out.println("create tuple");
    }
    catch (SQLException e) {
      System.out.println("Could not create tuple");
      e.printStackTrace();
    }
  }
  
  public void deleteTuple(String table, String condition) {
    try {
      Statement statement = this.conn.createStatement();
      String command="delete from "+table+" where "+condition;
      statement.executeUpdate(command);
      System.out.println("deleted tuple");
    }
    catch (SQLException e) {
      System.out.println("Could not delete tuple");
      e.printStackTrace();
    }
  }
  
  public ArrayList<Long> getTeamPlayerID(int team_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command="select Plays.player_id from "
          + "Plays inner join Team on Team.team_id=Plays.team_id where Plays.team_id="+team_id;
      ResultSet result = statement.executeQuery(command);
      ArrayList<Long> teaminfo = new ArrayList<Long>();
      while (result.next()) {
        teaminfo.add(result.getLong("player_id"));
      }
      if (teaminfo.size() == 0) {
        return null;
      }
      return teaminfo;
    }
    catch (SQLException e) {
      System.out.println("Could not retrieve team players information");
      e.printStackTrace();
      return null;
    }
  }
  
  public void leaveInstance(long player_id, long channel_id) {
    try {
      CallableStatement cs=this.conn.prepareCall("{Call leaveInstance("+player_id+", "+channel_id+")}");
      cs.executeUpdate();
      System.out.println("player left instance");
    }
    catch (SQLException e) {
      System.out.println("player could not leave instance");
      e.printStackTrace();
    }
  }

  public void createPlays(long player_id, long channel_id) {
    try {
      CallableStatement cs=this.conn.prepareCall("{Call createPlays("+player_id+", "+channel_id+")}");
      cs.executeUpdate();
      System.out.println("Created Plays for " + player_id + " in " + channel_id);
    }
    catch (SQLException e) {
      System.out.println("could not created Plays for " + player_id + " in " + channel_id);
      e.printStackTrace();
    }
  }

  public void deletePlays(long player_id, long channel_id) {
    try {
      CallableStatement cs=this.conn.prepareCall("{Call deletePlays("+player_id+", "+channel_id+")}");
      cs.executeUpdate();
      System.out.println("deleted Plays for " + player_id + " in " + channel_id);
    }
    catch (SQLException e) {
      System.out.println("could not delte Plays for " + player_id + " in " + channel_id);
      e.printStackTrace();
    }
  }

  public void createInvite(long sender, long receiver, long channel_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "INSERT IGNORE INTO Invite (sender,receiver,instance_id)"
          + " Values (" + sender + ", " + receiver + ", " + channel_id + ")";
      statement.executeUpdate(command);
      System.out.println("Created invite from to " + channel_id);
    }
    catch (SQLException e) {
      System.out.println("could not create invite for " + channel_id);
      e.printStackTrace();
    }
  }

  public void createTeam(long instance_id, String team_name) {
    try {
      Statement statement = this.conn.createStatement();
      String command="insert into Team (instance_id, team_name) values ("+instance_id+", '"+team_name+"')";
      statement.executeUpdate(command);
      System.out.println("Created team in database: " + team_name);
    }
    catch (SQLException e) {
      System.out.println("could not create instance in database: " + team_name);
      e.printStackTrace();
    }
  }
  
  public void createInstance(String privacy, String game_id, int freespot, long channel_id,
      long server_id,String instance_name) {
    try {
      String visibility;
      if (privacy.equals("private")) {
        visibility = "false";
      }
      else {
        visibility = "true";
      }
      CallableStatement cs=this.conn.prepareCall(
          "{Call createInstance("+visibility+", "+game_id+", "+freespot+", "+channel_id+", "+server_id+", '"+instance_name+"')}"
          );
      cs.executeUpdate();
      System.out.println("Created instance in database: " + instance_name);
    }
    catch (SQLException e) {
      System.out.println("could not create instance in database: " + instance_name);
      e.printStackTrace();
    }
  }

  public void deleteInstance(long channel_id) {
    try {
      CallableStatement cs=this.conn.prepareCall("{Call deleteInstance("+channel_id+")}");
      cs.executeUpdate();
      System.out.println("Deleted instance " + channel_id);
    }
    catch (SQLException e) {
      System.out.println("Error when deleting instance " + channel_id);
      e.printStackTrace();
    }
  }

  public String getIDbyNameGameType(String gametype) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select game_id from GameType where name='" + gametype + "'";
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

  public ArrayList<ArrayList<String>> getInvites(long player_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select * from ((Invite inner join Instance on "
          + "Invite.instance_id=Instance.instance_id) INNER JOIN Player ON Invite.sender = Player.player_id) where receiver=" + player_id;
      ResultSet result = statement.executeQuery(command);
      ArrayList<ArrayList<String>> invites = new ArrayList<ArrayList<String>>();
      while (result.next()) {
        ArrayList<String> toAdd = new ArrayList<String>();
        toAdd.add(result.getString("nickname"));
        toAdd.add(result.getString("server_id"));
        toAdd.add(result.getString("instance_id"));
        invites.add(toAdd);
      }
      if (invites.size() == 0) {
        return null;
      }
      return invites;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving instance record");
      e.printStackTrace();
      return null;
    }
  }
  
  public ArrayList<ArrayList<String>> getInstances(long server_id){
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select * from Instance where server_id="+server_id+"";
      ResultSet result = statement.executeQuery(command);
      ArrayList<ArrayList<String>> instances = new ArrayList<ArrayList<String>>();
      while (result.next()) {
        ArrayList<String> toAdd = new ArrayList<String>();
        toAdd.add(result.getString("instance_id"));
        toAdd.add(result.getString("free_spots"));
        toAdd.add(result.getString("public"));
        toAdd.add(result.getString("started"));
        instances.add(toAdd);
      }
      if (instances.size() == 0) {
        return null;
      }
      return instances;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving instance record");
      e.printStackTrace();
      return null;
    }
  }

  public ArrayList<ArrayList<String>> getGameTypeScores(String gametype, String limit){
    try {
      String direction=this.getGameTypeDirection(gametype);
      if (direction==null) {
        return null;
      }
      Statement statement = this.conn.createStatement();
      String command = "Select Player.nickname, Leaderboard.score from ((Leaderboard inner join GameType"
          + " on GameType.game_id=Leaderboard.game_id) inner join Player on Leaderboard.player_id=Player.player_id)"
          + " where GameType.name='"+gametype+"' order by Leaderboard.score "+direction+" "+limit;
      ResultSet result = statement.executeQuery(command);
      ArrayList<ArrayList<String>> scores = new ArrayList<ArrayList<String>>();
      while (result.next()) {
        ArrayList<String> toAdd = new ArrayList<String>();
        toAdd.add(result.getString("nickname"));
        toAdd.add(result.getString("score"));
        scores.add(toAdd);
      }
      if (scores.size() == 0) {
        return null;
      }
      return scores;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving user scores");
      e.printStackTrace();
      return null;
    }
  }
  
  public ArrayList<ArrayList<String>> getUserScores(long player_id){
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select GameType.name, Leaderboard.score from Leaderboard inner join GameType"
          + " on GameType.game_id=Leaderboard.game_id where Leaderboard.player_id="+player_id+""
              + " order by GameType.name Desc";
      ResultSet result = statement.executeQuery(command);
      ArrayList<ArrayList<String>> scores = new ArrayList<ArrayList<String>>();
      while (result.next()) {
        ArrayList<String> toAdd = new ArrayList<String>();
        toAdd.add(result.getString("name"));
        toAdd.add(result.getString("score"));
        scores.add(toAdd);
      }
      if (scores.size() == 0) {
        return null;
      }
      return scores;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving user scores");
      e.printStackTrace();
      return null;
    }
  }
  
  public ArrayList<Long> getAllPlayerInstance(long instance_id){
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select player_id from Plays where instance_id="+instance_id;
      ResultSet result = statement.executeQuery(command);
      ArrayList<Long> player_ids = new ArrayList<Long>();
      while (result.next()) {
        player_ids.add(result.getLong("player_id"));
      }
      if (player_ids.size() == 0) {
        return null;
      }
      return player_ids;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving all player id in given instance");
      e.printStackTrace();
      return null;
    }
    
  }
  
  public ArrayList<Long> getInstanceIDbyName(String name) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select instance_id from Instance where instance_name='" + name + "'";
      ResultSet result = statement.executeQuery(command);
      ArrayList<Long> ids = new ArrayList<Long>();
      while (result.next()) {
        ids.add(result.getLong("instance_id"));
      }
      if (ids.size() == 0) {
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

  public ArrayList<String> getRecordInstance(long channel_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select * from Instance where instance_id=" + channel_id;
      ResultSet result = statement.executeQuery(command);
      ArrayList<String> record = new ArrayList<String>();
      while (result.next()) {
        for (int i = 1; i < 8; i++) {
          record.add(result.getString(i));
        }
      }
      if (record.size() == 0) {
        return null;
      }
      return record;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving instance record");
      e.printStackTrace();
      return null;
    }
  }

  public ArrayList<String> getRecordInvite(long receiver, long channel_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select * from Invite inner join Instance on Invite.instance_id=Instance.instance_id "
          + "where Invite.instance_id=" + channel_id + " and receiver="+ receiver;
      ResultSet result = statement.executeQuery(command);
      ArrayList<String> record = new ArrayList<String>();
      while (result.next()) {
        for (int i = 1; i < 4; i++) {
          record.add(result.getString(i));
        }
      }
      if (record.size() == 0) {
        return null;
      }
      return record;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving instance record");
      e.printStackTrace();
      return null;
    }
  }

  public void setTeamIDPlays(long instance_id, long player_id, int team_id) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "update Plays set team_id="+team_id+" where instance_id="+instance_id+
          " and player_id="+player_id;
      statement.executeUpdate(command);
      System.out.println("set team succesfully");
    }
    catch (SQLException e) {
      System.out.println("Error when setting team");
      e.printStackTrace();
    }
  }
  
  public void setScore(long player_id, int game_id, int score, boolean highIsGood) {
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select score from Leaderboard where game_id="+game_id+" and player_id="+player_id;
      
      ResultSet result = statement.executeQuery(command);
      String db_score=null;
      while (result.next()) {
        db_score=result.getString("score");
      }
      
      String setScore="";
      if(db_score!=null) {
      if (Integer.parseInt(db_score)<score == highIsGood) {
        db_score=""+score;
      }
      setScore = "Update Leaderboard set score="+db_score+" where player_id="+player_id+" and game_id="+game_id;
      }
      else {
        db_score=""+score;
        setScore = "insert into Leaderboard (player_id, game_id, score) "
            + "Values ("+player_id+", "+game_id+", "+db_score+")";
      }
      
      
      statement.executeUpdate(setScore);
      
    }
    catch (SQLException e) {
      System.out.println("Error when setting score");
      e.printStackTrace();
    }
  }
  
  public void updateInstanceField(String table_name, String field_name, String field_value, long instance_id) {
    try {
      Statement statement = this.conn.createStatement();
      String update_field = "Update "+table_name+" set "+field_name+"="+field_value+" where instance_id="+instance_id;
      statement.executeUpdate(update_field);
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  public void updatePlaysTeamID(long instance_id, long player_id, String newTeamID) {
    try {
      Statement statement = this.conn.createStatement();
      String update_field = "update Plays set team_id="+newTeamID+" where instance_id="+instance_id+
          " and player_id="+player_id;
      statement.executeUpdate(update_field);
      System.out.println("updated teamid in play");
    }
    catch (SQLException e) {
      e.printStackTrace();
      System.out.println("could not updated teamid in play");
    }
  }
  
  public void updatePlayerInstanceField(String table_name, String field_name, String field_value, long player_id,long instance_id) {
    try {
      Statement statement = this.conn.createStatement();
      String update_field = "Update "+table_name+" set "+field_name+"="+field_value+" "
          + "where player_id="+player_id+" and instance_id="+instance_id;
      statement.executeUpdate(update_field);
    }
    catch (SQLException e) {
      e.printStackTrace();
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
  
  public ArrayList<Integer> getTeamIDInstance(long instance_id){
    try {
      Statement statement = this.conn.createStatement();
      String command = "Select team_id from Team where instance_id="+instance_id;
      ResultSet result = statement.executeQuery(command);
      ArrayList<Integer> record = new ArrayList<Integer>();
      while (result.next()) {
        record.add(result.getInt("team_id"));
      }
      if (record.size() == 0) {
        return null;
      }
      return record;
    }
    catch (SQLException e) {
      System.out.println("Error when retrieving team ids by instance id");
      e.printStackTrace();
      return null;
    }
  }
  
  public String getFieldWithConditionTable(String tablename, String field, String PrimaryCondition) {
    try {
      String command = "Select " + field + " from " + tablename+" where "+PrimaryCondition;
      ResultSet result = this.getResultSet(command);
      String output=null;
      while (result.next()) {
        output=result.getString(1);
      }
      return output;
    }
    catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve " + field + " from " + tablename+" where "+PrimaryCondition);
      e.printStackTrace();
      return null;
    }
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

  public int getNumMembers(long instanceID) {
    try {
      Statement statement = conn.createStatement();
      ResultSet num = statement.executeQuery(
          "SELECT count(*) FROM Plays WHERE instance_id = " + instanceID + " GROUP BY instance_id");
      num.next();
      return num.getInt(1);
    }
    catch (SQLException e) {
      if (e.getMessage().contains("empty result set")) {
        System.out.println("Empty result set");
        return 0;
      }
    }
    return -1;
  }
  
  public boolean getInstanceStarted(long instanceID) {
    try {
      Statement statement = conn.createStatement();
      ResultSet status = statement.executeQuery(
          "SELECT started FROM Instance WHERE instance_id = " + instanceID);
      status.next();
      return status.getBoolean(1);
    }catch(SQLException e) {
      System.out.println("Couldn't get instance status!");
      e.printStackTrace();
      return false;
    }
  }
  
  public String getGameTypeDirection(String gametype) {
    try {
      Statement statement = conn.createStatement();
      ResultSet direction = statement.executeQuery(
          "SELECT sorting_direction FROM GameType WHERE name = '" + gametype+"'");
      String result=null;
      while(direction.next()) {
        result=direction.getString(1);
      }
      return result;
    }catch(SQLException e) {
      System.out.println("Couldn't get direction");
      e.printStackTrace();
      return "Desc";
    }
  }
  
  public ArrayList<String> getInstanceField(String tablename, String field, Long instanceID) {
    try {
      String command = "Select " + field + " from " + tablename+" WHERE instance_id="+instanceID;
      ResultSet result = this.getResultSet(command);
      ArrayList<String> array = new ArrayList<String>();
      while (result.next()) {
        array.add(result.getString(field));
      }
      return array;
    }
    
    catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve " + field + " from " + tablename+" at instance "+instanceID);
      e.printStackTrace();
      return null;
    }
  }
  
  public String getInstancedPlayerField(String tablename, String field, Long instanceID, Long playerID) {
    try {
      String command = "Select " + field + " from " + tablename+" WHERE instance_id="+instanceID+" AND player_id = "+playerID;
      ResultSet result = this.getResultSet(command);
      result.next();
      return result.getString(1);
    }
    
    catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve " + field + " from " + tablename+" at instance "+instanceID+" for player "+playerID);
      e.printStackTrace();
      return null;
    }
  }
  
  public ArrayList<String> getRandomTuple(String tablename) {
    try {
      String command = "Select * from " + tablename+" ORDER BY RAND() LIMIT 1";
      ResultSet result = this.getResultSet(command);
      result.next();
      System.out.println("received tuple");
      ArrayList<String> array = new ArrayList<String>();
      int count=1;
      while(true) {
        try {
          array.add(result.getString(count));
          count++;
        }catch(Exception e) {
          break;
        }
      }
      System.out.println(array);
      return array;
    }
    
    catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve tuple from " + tablename);
      e.printStackTrace();
      return null;
    }
  }

  public void closeConnection() throws SQLException {
    this.conn.close();
  }
}