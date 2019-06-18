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
    System.out.println(instanceID);
    Long sender = message.getAuthor().getIdLong();
    HashMap<Long, Integer> rolls = globalRolls.get(instanceID);
    TextChannel channel = message.getTextChannel();
    if (message.getMessage().getContentStripped().equalsIgnoreCase("roll")) {
      if (rolls.containsKey(sender)) {
        channel.sendMessage("Hey! "+message.getAuthor().getName()+"! You don't get to roll again!").queue();
      }
      else {
        int roll = rand.nextInt(100);
        rolls.put(sender, roll);
        channel.sendMessage(message.getAuthor().getName() + " rolled " + roll).queue();
        
        // Check for game end condition
        if(rolls.size() == db.getNumMembers(instanceID)) {
          this.endGame(channel, instanceID, rolls, true);
        }
      }
    }
    else {
      channel.sendMessage("Roll my child.").queue();
    }

  }

  boolean canPause(Long instanceID) {
    return true;
  }

  String start(String[] arguments, Long instanceID, TextChannel channel) throws Exception {
    System.out.println(instanceID);
    globalRolls.put(instanceID, new HashMap<Long, Integer>());
    return "Welcome to 1d100! The rules are simple! Type \"!roll\" to roll a d100! You only get one try, tho.";
  }

}
