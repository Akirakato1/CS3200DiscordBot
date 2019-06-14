import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;

// Handles commands from Instance threads.
public class InstanceCommandManager {
  /*
  Database db;
  public GeneralCommandManager(Database db) {
    this.db = db;
  }
  */
  
  // Process a command. Assume the command begins with the proper delimiter.
  public void processCommand(MessageReceivedEvent commandEvent) {
    String[] words = commandEvent.getMessage().getContentStripped().split(" ");
    // Determine the command.
    String command = words[0].substring(1);

    if (command.equals("invite")) {
      // Invite command.
      List<Member> invited = commandEvent.getMessage().getMentionedMembers();
      String senderName = commandEvent.getAuthor().getName();
      String channelName = commandEvent.getChannel().getName();
      for (Member m : invited) {
        // TODO: Add the invite to the SQL database.
        sendPrivateMessage(m.getUser(), senderName + " invited you to join " + channelName);
      }
    }
    else if (command.equals("leave")) {
      // Leave the instance
      if (commandEvent.getTextChannel().getMembers().size() == 1) {
        // This is the last member. Delete the channel.
        commandEvent.getTextChannel().delete();
      } else {
        // Not the last member. Simply remove permissions.
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        permissions.add(Permission.MESSAGE_READ);
        permissions.add(Permission.MESSAGE_WRITE);
        permissions.add(Permission.VIEW_CHANNEL);
        PermissionOverrideAction override = commandEvent.getTextChannel()
            .createPermissionOverride(commandEvent.getMember());
        override.setDeny(permissions);
        override.queue();
      }
    }
    else if (command.equals("start")) {
      //TODO: Implement database functions and game functions
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
