import java.util.HashMap;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

// Abstract class for objects that handle game events. Contains helpers for managing pausing and game end states.
public abstract class GameManager extends CommandManager {
  HashMap<Long, Boolean> status;

  public GameManager(Database db) {
    super(db);
    status = new HashMap<Long, Boolean>();
  }

  public void processCommand(MessageReceivedEvent commandEvent) {
    Long channelID = commandEvent.getChannel().getIdLong();

    if (commandEvent.getMessage().getContentStripped().equals("!pause")) {
      // Command is to toggle pause state.
      togglePause(channelID);
    }
    else if (!status.containsKey(channelID)) {
      // Game is not paused or finished. Update the game state.
      update(commandEvent);
    }

  }

  public abstract void update(MessageReceivedEvent message);

  public boolean checkAndApplyFinished(Long instanceID) {
    Boolean threadState = status.get(instanceID);
    if (threadState == null) {
      return false;
    }
    if (threadState.booleanValue()) {
      // Finished
      status.remove(instanceID);
      return true;
    }
    return false;
  }

  protected void togglePause(Long instanceID) {
    Boolean threadState = status.get(instanceID);

    if (threadState == null) {
      // Not paused and not finished
      status.put(instanceID, false);
    }
    else if (!threadState.booleanValue()) {
      // Paused and not finished
      status.remove(instanceID);
    }
  }
  
  abstract boolean canPause(Long instanceID);
  
  // Code for starting the game. If an argument is invalid, throw an error following this format:
  // "[invalid index]:description"
  abstract String start(String[] arguments, Long instanceID) throws Exception;
  
  protected void quit(Long instanceID) {}
  
  protected void endGame(Long instanceID, HashMap<Long, Integer> scores, boolean highIsGood) {
    quit(instanceID);
    int gameType = Integer.parseInt(db.getInstanceField("Instance", "game_id", instanceID).get(0));
    for(Long player: scores.keySet()) {
      db.setScore(player, gameType, scores.get(player), highIsGood);
    }
    
    // Add finished thread to status list.
    status.put(instanceID, true);
  }

}
