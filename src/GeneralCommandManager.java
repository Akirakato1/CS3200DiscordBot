import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;

// Handles commands from the General thread(s).
public class GeneralCommandManager {
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
    TextChannel channel = commandEvent.getTextChannel();

    // List all the channels on the server.
    // TODO: Remove testing command.
    if (command.equals("channelnames")) {
      List<Channel> channels = commandEvent.getGuild().getChannels();

      String channelnames = "";
      for (Channel c : channels) {
        channelnames = channelnames + c.getName() + " \n";
      }
      channel.sendMessage("Channel names: " + channelnames).queue();
    }

    if (command.equals("createchannel")) {
      // Create a new channel
      String channelname = words[1];
      for (int i = 2; i < words.length; i++) {
        channelname = channelname + "-" + words[i];
      }

      String parent = "Testing Channels";

      List<Channel> channels = commandEvent.getGuild().getChannels();

      boolean nameused = false;
      if (channels.size() > 0) {
        for (Channel c : channels) {
          if (channelname.equalsIgnoreCase(c.getName())) {
            nameused = true;
            break;
          }
        }
      }
      
      if (!nameused) {
        ChannelAction channelaction = commandEvent.getGuild().getController()
            .createTextChannel(channelname);
        channelaction.setTopic("Testing Channels");
        channelaction.setParent(commandEvent.getGuild().getCategoriesByName(parent, true).get(0));
        // Set public permissions (only members may view channel)
        ArrayList<Permission> publicDeny = new ArrayList<Permission>();
        publicDeny.addAll(Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS));
        publicDeny.add(Permission.VIEW_CHANNEL);
        channelaction.addPermissionOverride(commandEvent.getGuild().getPublicRole(),
            new ArrayList<Permission>(), publicDeny);
        // Set author permissions
        ArrayList<Permission> authorAllow = new ArrayList<Permission>();
        authorAllow.add(Permission.VIEW_CHANNEL);
        authorAllow.add(Permission.MESSAGE_READ);
        authorAllow.add(Permission.MESSAGE_WRITE);
        channelaction.addPermissionOverride(commandEvent.getMember(), authorAllow,
            new ArrayList<Permission>());

        channelaction.queue();
        channel.sendMessage("Created channel: " + channelname);
      }
      else {
        channel.sendMessage("name already used").queue();
      }
    }
    else if (command.contentEquals("join")) {
      // Request to join an instance

      // TODO: Check for permission to join instance

      // Join the channel
      // TODO: Search Database for channel id using name
      /*
      TextChannel tc = commandEvent.getGuild().getTextChannelById(id);
      PermissionOverrideAction poa = tc.createPermissionOverride(commandEvent.getMember());
      ArrayList<Permission> permissions = new ArrayList<Permission>();
      permissions.add(Permission.MESSAGE_READ);
      permissions.add(Permission.MESSAGE_WRITE);
      permissions.add(Permission.VIEW_CHANNEL);
      poa.setAllow(permissions);
      poa.queue();
      */
    }

  }
}
