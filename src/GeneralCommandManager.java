import java.util.List;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;

// Handles commands from the General thread(s).
public class GeneralCommandManager {

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
      String channelname = words[1];
      for (int i = 2; i < words.length; i++) {
        channelname = channelname + "-" + words[i];
      }

      String parent = "Testing Channels";

      List<Channel> channels = commandEvent.getGuild().getChannels();

      boolean nameused = false;

      for (Channel c : channels) {
        if (channelname.equalsIgnoreCase(c.getName())) {
          nameused = true;
          break;
        }
      }
      ChannelManager channelmanager = null;
      if (!nameused) {
        ChannelAction channelaction = commandEvent.getGuild().getController()
            .createTextChannel(channelname);
        channelaction.setTopic("Testing Channels");
        channelaction.setParent(commandEvent.getGuild().getCategoriesByName(parent, true).get(0));
        //channelaction.addPermissionOverride(commandEvent.getGuild().getPublicRole(), 0, deny)
        channelaction.queue();
        
      }
      else {
        channel.sendMessage("name already used").queue();
      }
      
      for (Channel c : channels) {
        if (c.getName().equals(channelname)) {
          channelmanager = channels.get(0).getManager();
        }
      }
      // TODO: Edit channel settings
      if(channelmanager == null) {
        System.out.println("Ugh. Couldn't find a siutable channel manager.");
      }
    }
  }
}
