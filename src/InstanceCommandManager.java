import java.util.ArrayList;
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
public class InstanceCommandManager extends CommandManager{
  
  ArrayList<CommandManager> gameManagers; // gameManagers by GameID
  HashMap<Long, Integer> gameThreads; // Maps threads that have started a game to the corresponding GameID

  public InstanceCommandManager(Database db) {
    super(db);
    commands.add(new String[] { "!help", "context(optional)" });
    commands.add(new String[] { "!invite", "[\\@recipient(s)]" });
    commands.add(new String[] { "!leave" });
    commands.add(new String[] { "!start" });
  }

  // Process a command. Assume the command begins with the proper delimiter.
  public void processCommand(MessageReceivedEvent commandEvent) {
    System.out.println("Recieved instance command");
    String content = commandEvent.getMessage().getContentStripped();
    String command = content.substring(1);
    if (content.contains(":")) {
      command = command.substring(0, content.indexOf(":") - 1);
    }
    String[] arguments = content.substring(content.indexOf(":") + 1).split(",");
    // Strip whitespace from argument sides
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = arguments[i].trim();
    }

    TextChannel channel = commandEvent.getTextChannel();

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
      
      if (db.getNumMembers(commandEvent.getChannel().getIdLong())== 0) {
        System.out.println("Deleting channel");
        // This is the last member. Delete the channel.
        db.deleteInstance(channel.getIdLong());
        commandEvent.getTextChannel().delete().queue();
      }
      else {
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
    }
    else if(command.equals("help")) {
      channel.sendMessage(help(arguments)).queue();
    }
  }

  public void sendPrivateMessage(User user, String content) {
    // openPrivateChannel provides a RestAction<PrivateChannel>
    // which means it supplies you with the resulting channel
    user.openPrivateChannel().queue((channel) -> {
      channel.sendMessage(content).queue();
    });
  }
}
