import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
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
  abstract String start(String[] arguments, Long instanceID, TextChannel channel) throws Exception;
  
  protected void quit(Long instanceID, TextChannel channel, HashMap<Long, Integer> scores) {}
  
  protected void endGame(TextChannel channel, Long instanceID, HashMap<Long, Integer> scores, boolean highIsGood) {
    quit(instanceID, channel, scores);
    Long winnerID = scores.keySet().iterator().next(); // default to p1
    int bestScore = scores.get(winnerID); // default to p1's score
    int gameType = Integer.parseInt(db.getInstanceField("Instance", "game_id", instanceID).get(0));
    
    for(Long player: scores.keySet()) {
      int score = scores.get(player);
      db.setScore(player, gameType, score, highIsGood);
      if(highIsGood == score > bestScore) {
        bestScore = score;
        winnerID = player;
      }
      
    }
    System.out.println(winnerID);
    Member winner = channel.getGuild().getMemberById(winnerID);
    String winnerName = winner.getEffectiveName();
    channel.sendMessage("Congratulations, "+winnerName+"!\nYou won with "+bestScore+" points!").queue();
    // Add finished thread to status list.
    status.put(instanceID, true);
  }
  
  // Returns the minimum number of teams for this game.
  public int getMinTeams() {
    return 0;
  }
  
  // Returns {team_id, team_name} of the top scoring team
  protected String[] getTopTeam(Long instanceID, HashMap<Long, Integer> scores) {
    ArrayList<String> players = db.getInstanceField("Plays", "player_id", instanceID);
    ArrayList<String> teamIDs;
    HashMap<String, Integer> teamScores = new HashMap<String, Integer>();
    HashMap<String, String> teamNames = new HashMap<String, String>();
    // Get team info
    teamIDs = db.getInstanceField("Team", "team_id", instanceID);
    for(String tID : teamIDs) {
      teamNames.put(tID, db.getFieldWithConditionTable("Team", "team_name", "team_id="+tID));
      teamScores.put(tID, 0);
    }
    
    // Loop over players and sum team scores
    String topTeam = teamIDs.get(0);
    int topTeamScore = 0;
    for(String pID : players) {
      Long playerID = Long.parseLong(pID);
      String teamID = db.getInstancedPlayerField("QuizBowl_Player", "team_id", instanceID, playerID);
      // Iterate team score
      int newTeamScore = teamScores.get(teamID)+scores.get(Long.parseLong(pID));
      teamScores.put(teamID, newTeamScore);
      // Update top team if applicable
      if(newTeamScore > topTeamScore) {
        topTeamScore = newTeamScore;
        topTeam = teamID;
      }
    }
    // Convert topTeam from team_id to team_name
    return new String[] {topTeam, teamNames.get(topTeam)};
  }

}
