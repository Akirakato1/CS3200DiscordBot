import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

public class Database {

  private final String userName;
  private final String password;
  private final String serverName;
  private final int portNumber;
  private final String dbName;
  private Connection conn;
 
  Database(String un,String pw, String server, int port, String db) throws SQLException {
    this.userName=un;
    this.password=pw;
    this.serverName=server;
    this.portNumber=port;
    this.dbName=db;
    try {
    this.conn=this.getConnection();
    System.out.println("Connection Established");
    } catch (SQLException e) {
      System.out.println("ERROR: Could not connect to the database");
      e.printStackTrace();
      return;
    }
  }
  
  public Connection getConnection() throws SQLException {
    Connection conn = null;
    Properties connectionProps = new Properties();
    connectionProps.put("user", this.userName);
    connectionProps.put("password", this.password);

    conn = DriverManager.getConnection("jdbc:mysql://"
        + this.serverName + ":" + this.portNumber + "/" + this.dbName,
        connectionProps);

    return conn;
  }

  public boolean executeUpdate(Connection conn, String command) throws SQLException {
      Statement stmt = null;
      try {
          stmt = conn.createStatement();
          stmt.executeUpdate(command); // This will throw a SQLException if it fails
          return true;
      } finally {

        // This will run whether we throw an exception or not
          if (stmt != null) { stmt.close(); }
      }
  }
  
  
  public ResultSet getResultSet(String command) throws SQLException {
    PreparedStatement statement=this.conn.prepareStatement(command);
    ResultSet result=statement.executeQuery();
    return result;
  }
  public ArrayList<String> getFieldFromTable(String tablename,String field){
    try {
    String command="Select "+field+" from "+ tablename;
    ResultSet result=this.getResultSet(command);
    ArrayList<String> array=new ArrayList<String>();
    while(result.next()) {
      array.add(result.getString(field));
    }
    return array;
    } catch (SQLException e) {
      System.out.println("ERROR: Could not retrieve "+ field+" from "+tablename);
      e.printStackTrace();
      return null;
    }
  }
  
  public void closeConnection() throws SQLException {
    this.conn.close();
  }
}