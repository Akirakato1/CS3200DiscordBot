import java.util.HashMap;
import java.util.Random;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class DOneHundredManager extends GameManager {
  HashMap<Long, HashMap<Long, Integer>> globalRolls;
  Random rand;

  public DOneHundredManager(Database db) {
    super(db);
    globalRolls = new HashMap<Long, HashMap<Long, Integer>>();
    rand = new Random();
  }

  @Override
  public void update(MessageReceivedEvent message) {
    Long instanceID = message.getChannel().getIdLong();
    Long sender = message.getAuthor().getIdLong();
    HashMap<Long, Integer> rolls = globalRolls.get(instanceID);
    TextChannel channel = message.getTextChannel();
    if (message.getMessage().getContentStripped().equalsIgnoreCase("roll")) {
      if (rolls.containsKey(sender)) {
        channel.sendMessage("Hey! "+message.getAuthor().getName()+"! You don't get to roll again!");
      }
      else {
        int roll = rand.nextInt(100);
        rolls.put(sender, roll);
        channel.sendMessage(message.getAuthor().getName() + " rolled " + roll);
        
        // Check for game end condition
        if(rolls.size() == db.getNumMembers(instanceID)) {
          this.endGame(instanceID, rolls, true);
        }
      }
    }
    else {
      channel.sendMessage("Roll my child.");
    }

  }

  boolean canPause(Long instanceID) {
    return true;
  }

  void start(String[] arguments, Long instanceID) throws Exception {
    globalRolls.put(instanceID, new HashMap<Long, Integer>());
  }

}
