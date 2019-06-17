import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;

// Handles commands from Instance threads.
public class InstanceCommandManager extends CommandManager {

  ArrayList<GameManager> gameManagers; // gameManagers by GameID
  HashMap<Long, Integer> gameThreads; // Maps threads that have started a game to the corresponding
                                      // GameID

  public InstanceCommandManager(Database db) {
    super(db);
    // Populate help function
    commands.add(new String[] { "!help", "context(optional)" });
    commands.add(new String[] { "!invite", "[\\@recipient(s)]" });
    commands.add(new String[] { "!leave" });
    commands.add(new String[] { "!start" });
    
    // Initialize GameManagers
    gameManagers = new ArrayList<GameManager>();
    gameManagers.add(new QuizBowlManager(db));
  }

  // Process a command. Assume the command begins with the proper delimiter.
  public void processCommand(MessageReceivedEvent commandEvent) {
    System.out.println("Recieved instance command");
    String content = commandEvent.getMessage().getContentStripped();
    String command = content.substring(1);
    TextChannel channel = commandEvent.getTextChannel();
    // Separate command from arguments
    if (content.contains(":")) {
      command = command.substring(0, content.indexOf(":") - 1);
    }
    // Put arguments in an ArrayList
    String[] arguments = getArguments(content, channel, command);
    
    

    if (command.equals("invite")) {
      // Invite command.
      List<Member> invited = commandEvent.getMessage().getMentionedMembers();
      String senderName = commandEvent.getAuthor().getName();
      String channelName = commandEvent.getChannel().getName();
      for (Member m : invited) {
        // TODO: Add the invite to the SQL database.
        sendPrivateMessage(m.getUser(), senderName + " invited you to join " + channelName);
        db.createInvite(commandEvent.getAuthor().getIdLong(), m.getUser().getIdLong(),
            channel.getIdLong());
      }
    }
    else if (command.equals("leave")) {
      // Leave the instance
      System.out.println(
          "Members left before leaving: " + commandEvent.getTextChannel().getMembers().size());
      db.leaveInstance(commandEvent.getAuthor().getIdLong(), commandEvent.getChannel().getIdLong());

      if (db.getNumMembers(commandEvent.getChannel().getIdLong()) == 0) {
        System.out.println("Deleting channel");
        // This is the last member. Delete the channel.
        db.deleteInstance(channel.getIdLong());
        commandEvent.getTextChannel().delete().queue();
      }
      else {
        channel.sendMessage(commandEvent.getAuthor().getName()+" has left your struggle. \nRemaining free spots: "+
            db.getInstanceField("Instance", "free_spots", channel.getIdLong()).get(0)).queue();
            
        System.out.println("Removing permissions");
        // Not the last member. Simply remove permissions.
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        permissions.add(Permission.MESSAGE_READ);
        permissions.add(Permission.MESSAGE_WRITE);
        permissions.add(Permission.VIEW_CHANNEL);
        permissions.add(Permission.ADMINISTRATOR);
        PermissionOverrideAction override = commandEvent.getTextChannel()
            .putPermissionOverride(commandEvent.getMember());
        override.setDeny(permissions);
        override.queue();
      }
    }
    else if (command.equals("start")) {
      // TODO: Implement database functions and game functions
      // Find the correct gameManager to send the request
      // Call start(arguments)
      // Catch and display exception on failure
      // Add to the redirection cache on success
    }
    else if (command.equals("help")) {
      channel.sendMessage(help(arguments)).queue();
    }
    else {
      int suggested = suggest(command);
      String suggestion = "";
      if(suggested == -1) {
        suggestion = "We couldn't figure out what you were going for there.\nType !help for a list of commands.";
      }else {
        suggestion = "did you mean: "+help(new String[] {commands.get(suggested)[0]});
      }
      channel.sendMessage("Invalid command. "+suggestion).queue();
    }
  }

  public void instanceMessage(MessageReceivedEvent event) {
    long key = event.getChannel().getIdLong();
    if (!gameThreads.isEmpty()) {
      if (gameThreads.containsKey(key)) {
        // It's a game event! Process immediately!
        gameManagers.get(gameThreads.get(key)).processCommand(event);
        
        // Check if the game is over
        if(gameManagers.get(gameThreads.get(key)).checkAndApplyFinished(key)) {
          // Remove from cache
          gameThreads.remove(key);
          //TODO: set gameStarted to false on db

        }
      }
    }
    
    // Not sure if the instance has started. Database query for it.
    if(db.getInstanceStarted(event.getChannel().getIdLong())) {
      // Game is started. Add the missing information to the cache.
      int gameID = Integer.parseInt(db.getIDbyNameGameType(event.getTextChannel().getTopic()));
      gameThreads.put(key, gameID-1);
      gameManagers.get(gameThreads.get(key)).processCommand(event);
    }
  }

}
