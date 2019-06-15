import java.sql.SQLException;
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
  ArrayList<String[]> commands;
  Database db;
  public GeneralCommandManager(Database db) {
    this.commands = new ArrayList<String[]>();
    commands.add(new String[] {"!help", "context(optional)"});
    commands.add(new String[]{"!channelnames"});
    commands.add(new String[]{"!createinstance", "[public | private]", "gametype", "capacity", "name"});
    
    this.db = db;
  }

  // Process a command. Assume the command begins with the proper delimiter.
  public void processCommand(MessageReceivedEvent commandEvent) {
    System.out.println("Recieved general command");
    String content = commandEvent.getMessage().getContentStripped();
    String command = content.substring(1);
    if(content.contains(":")) {
      command = command.substring(0, content.indexOf(":")-1);
    }
    String[] arguments = content.substring(content.indexOf(":")+1).split(",");
    // Strip whitespace from argument sides
    for(int i = 0; i < arguments.length; i++) {
      arguments[i] = arguments[i].trim();
    }
    
    String[] words = commandEvent.getMessage().getContentStripped().split(" ");
    // Determine the command.
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

    else if (command.equals("createinstance")) {
      // Create a new channel
      // !createinstance: public | private, 
      
      // Check for public/private permissions
      String privacy = "";
      if(arguments[0].equalsIgnoreCase("public")) {
        // public
        privacy = "public";
      } else if(arguments[0].equalsIgnoreCase("private")) {
        // private
        privacy = "private";
      } else {
        String description = arguments[0]+" should be either \"public\" or \"private\"";
        channel.sendMessage(errorMessage(command, arguments, 0, description));
        return;
      }
      
      String gameid = db.getIDbyNameGameType(arguments[1]);
      
      System.out.println("game id: "+gameid);
      
      if(gameid.equals("invalid gametype")) {
        String description = arguments[1]+" is not a valid gametype.";
        channel.sendMessage(errorMessage(command, arguments, 1, description));
        return;
      }else if(gameid.equals("no id")) {
        String description = "Something has gone terribly wrong with the gametype query. Does the gametype contain quotes?";
        channel.sendMessage(errorMessage(command, arguments, 1, description));
        return;
      }
      
      
      int capacity = 0;
      try {
        capacity = Integer.parseInt(arguments[2]);
      }catch(NumberFormatException e) {
        String description = arguments[2]+" is not an integer.";
        channel.sendMessage(errorMessage(command, arguments, 2, description));
        return;
      }
      
      
      String channelname = arguments[3].replace(" ", "-");
      
      
      // public | private, game_id (name), open slots, channel name
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
        
        // Begin creating the Discord channel
        ChannelAction channelaction = commandEvent.getGuild().getController()
            .createTextChannel(channelname);
        channelaction.setTopic("Testing Channels");
        channelaction.setParent(commandEvent.getGuild().getCategoriesByName(parent, true).get(0));
        
        // Set public permissions (only members may view channel)
        ArrayList<Permission> publicDeny = new ArrayList<Permission>();
        publicDeny.addAll(Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS));
        publicDeny.add(Permission.VIEW_CHANNEL);
        publicDeny.add(Permission.ADMINISTRATOR);
        channelaction.addPermissionOverride(commandEvent.getGuild().getPublicRole(),
            new ArrayList<Permission>(), publicDeny);
        
        // Set author permissions
        ArrayList<Permission> authorAllow = new ArrayList<Permission>();
        authorAllow.add(Permission.VIEW_CHANNEL);
        authorAllow.add(Permission.MESSAGE_READ);
        authorAllow.add(Permission.MESSAGE_WRITE);
        channelaction.addPermissionOverride(commandEvent.getMember(), authorAllow,
            new ArrayList<Permission>());

        Channel newChannel = channelaction.complete();
        channel.sendMessage("Created channel: " + channelname);
        
        // Apply it to the database
        
        db.createInstance(privacy, gameid, capacity, newChannel.getIdLong(), channelname);
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
    else if (command.contentEquals("help")) {
      
      
      
      String message = "";
      if(arguments.length == 0) {
        for(String[] function : commands) {
          message += ", !"+function[0];
        }
        message = message.substring(2);
      }
    } else {
      // Invalid command, commence suggestions?
    }

  }
  
  private String errorMessage(String command, String[] arguments, int invalid, String description) {
    String message = "Error in !"+command+": ";
    for(int i = 0; i < arguments.length; i++) {
      if(i > 0) {
        message = message.concat(", ");
      }
      if(i == invalid) {
        message = message.concat("```diff ");
      }
      message = message.concat(arguments[i]);
      if(i == invalid) {
        message = message.concat("```");
      }
    }
    message = message.concat("\n"+description);
    
    return message;
  }
}
