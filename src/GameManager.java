import java.util.ArrayList;

public abstract class GameManager extends CommandManager{
  ArrayList<Long> finished;
  ArrayList<Long> paused;
  
  public GameManager(Database db) {
    super(db);
  }
  
  public boolean checkAndApplyFinished(Long instanceID) {
    if(finished.contains(instanceID)) {
      finished.remove(instanceID);
      return true;
    }
    return false;
  }
}
