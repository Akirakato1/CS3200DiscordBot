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
  
  abstract void start(String[] arguments) throws Exception;
  
  protected void endGame(Long instanceID, HashMap<Long, Integer> scores, boolean highIsGood) {
    //TODO: Update leaderboard using score info.
    
    // Add finished thread to status list.
    status.put(instanceID, true);
  }

}
