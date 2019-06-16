import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;

// Handles commands from the General thread(s).
public class GeneralCommandManager extends CommandManager{

  public GeneralCommandManager(Database db) {
    super(db);
    commands.add(new String[] { "help", "context(optional)" });
    commands.add(new String[] { "channelnames" });
    commands.add(
        new String[] { "createinstance", "[public | private]", "gametype", "capacity", "name" });
    commands.add(new String[] { "join", "instance name" });
    commands.add(new String[] { "myinvites" });
    commands.add(new String[] { "instances" });
    commands.add(new String[] { "myhighscores" });
    commands.add(new String[] { "leaderboard", "gametype", "limitnumber(optional)" });
  }

  // Process a command. Assume the command begins with the proper delimiter.
  public void processCommand(MessageReceivedEvent commandEvent) {
    System.out.println("Recieved general command");
    String content = commandEvent.getMessage().getContentStripped();
    String command = content.substring(1);
    TextChannel channel = commandEvent.getTextChannel();
    // Separate command from arguments
    if (content.contains(":")) {
      command = command.substring(0, content.indexOf(":") - 1);
    }
    // Put arguments in an ArrayList
    String[] arguments = getArguments(content, channel, command);

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
    else if(command.equals("myinvites")) {
      ArrayList<ArrayList<String>> invites=this.db.getInvites(commandEvent.getAuthor().getIdLong());
      String message="";
      if(invites==null) {
        sendPrivateMessage(commandEvent.getAuthor(), "No invites, go make friends");
        return;
      }
      for(ArrayList<String> invite: invites) {
        Guild guild=commandEvent.getJDA().getGuildById(Long.parseLong(invite.get(1)));
        TextChannel channel_invite=guild.getTextChannelById(invite.get(2));
        message=message+"\nAn invitation from "+invite.get(0)+" to join instance "+
        channel_invite.getName()+" in server "+ guild.getName();
      }
      sendPrivateMessage(commandEvent.getAuthor(),message);
      return;
    }
    else if (command.equals("myhighscores")) {
      ArrayList<ArrayList<String>> scores=this.db.getUserScores(commandEvent.getAuthor().getIdLong());
      String message="";
      if(scores==null) {
        channel.sendMessage("You have no scores").queue();
        return;
      }
      for(ArrayList<String> score:scores) {
        message=message+"**Game Type: **"+score.get(0)+"** Score: **"+score.get(1);
      }
      channel.sendMessage(message).queue();
      return;
    }
    else if (command.equals("leaderboard")) {
      if(arguments.length==0) {
        channel.sendMessage("Invalid number of arguments").queue();
        return;
      }
      String limit="";
      if(arguments.length==2) {
        limit="LIMIT "+arguments[1];
      }
      ArrayList<ArrayList<String>> scores=this.db.getGameTypeScores(arguments[0],limit);
      
      String message="";
      if(scores==null) {
        channel.sendMessage("This gametype has no scores").queue();
        return;
      }
      for(ArrayList<String> score:scores) {
        message=message+"**Player: **"+score.get(0)+"** Score: **"+score.get(1);
      }
      channel.sendMessage(message).queue();
      return;
    }
    else if (command.equals("instances")) {
      ArrayList<ArrayList<String>> instances=this.db.getInstances(commandEvent.getGuild().getIdLong());
      String message="";
      if(instances==null) {
        channel.sendMessage("No Instances Up right now").queue();
        return;
      }
      for(ArrayList<String> instance: instances) {
        Guild guild=commandEvent.getGuild();
        TextChannel textc=guild.getTextChannelById(instance.get(0));
        String freespots=instance.get(1);
        String visibility="private";
        String started="Not Started";
        if(Integer.parseInt(instance.get(1))<0) {
          freespots="infinte";
        }
        if(Integer.parseInt(instance.get(2))==1) {
          visibility="public";
        }
        if(Integer.parseInt(instance.get(3))==1) {
          visibility="Started";
        }
        message=message+"\n**Instance Type:** "+textc.getParent().getName()+" **Instance name:** "+textc.getName()+" **Free spots:** "+freespots
        +" **"+visibility+"** "+started;
      }
      channel.sendMessage(message).queue();
      return;
    }
    else if (command.equals("createinstance")) {
      // Create a new channel
      if(arguments.length != 4) {
        channel.sendMessage("Error: createinstance expects 4 arguments.").queue();
        return;
      }
      
      // !createinstance: public | private,

      // Check for public/private permissions
      String privacy = "";
      if (arguments[0].equalsIgnoreCase("public")) {
        // public
        privacy = "public";
      }
      else if (arguments[0].equalsIgnoreCase("private")) {
        // private
        privacy = "private";
      }
      else {
        String description = arguments[0] + " should be either \"public\" or \"private\"";
        channel.sendMessage(errorMessage(command, arguments, 0, description)).queue();
        return;
      }

      String gameid = db.getIDbyNameGameType(arguments[1]);

      System.out.println("game id: " + gameid);

      if (gameid.equals("invalid gametype")) {
        String description = arguments[1] + " is not a valid gametype.";
        channel.sendMessage(errorMessage(command, arguments, 1, description)).queue();
        return;
      }
      else if (gameid.equals("no id")) {
        String description = "Something has gone terribly wrong with the gametype query. Does the gametype contain quotes?";
        channel.sendMessage(errorMessage(command, arguments, 1, description)).queue();
        return;
      }

      int capacity = 0;
      try {
        capacity = Integer.parseInt(arguments[2]);
      }
      catch (NumberFormatException e) {
        String description = arguments[2] + " is not an integer.";
        channel.sendMessage(errorMessage(command, arguments, 2, description)).queue();
        return;
      }

      String channelname = arguments[3].replace(" ", "-");
      
      // public | private, game_id (name), open slots, channel name
      String parent = arguments[1];

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
        channel.sendMessage("Created channel: " + channelname).queue();

        // Apply it to the database

        db.createInstance(privacy, gameid, capacity, newChannel.getIdLong(), commandEvent.getGuild().getIdLong(),channelname);
        db.joinInstance(commandEvent.getAuthor().getIdLong(), newChannel.getIdLong());
      }
      else {
        channel.sendMessage("name already used").queue();
      }
    }
    else if (command.contentEquals("join")) {

      ArrayList<Long> ids = this.db.getInstanceIDbyName(arguments[0]);
      List<TextChannel> channels = channel.getGuild().getTextChannels();
      TextChannel target_channel = null;
      long channel_id = 0;
      if (ids == null) {
        channel.sendMessage("invalid instance name").queue();
        return;
      }

      for (long id : ids) {
        for (int i = 0; i < channels.size(); i++) {
          if (channels.get(i).getIdLong() == id) {
            target_channel = channels.get(i);
            channel_id = target_channel.getIdLong();
          }
        }
      }

      if (target_channel == null) {
        channel.sendMessage("invalid instance name").queue();
        return;
      }

      List<Member> members_of_target_channel = target_channel.getMembers();

      for (int i = 0; i < members_of_target_channel.size(); i++) {
        if (commandEvent.getAuthor().getIdLong() == members_of_target_channel.get(i).getUser()
            .getIdLong()) {
          channel.sendMessage("You're already in the Instance").queue();
          return;
        }
      }

      // instance_id instance_name free_spots game_id public
      ArrayList<String> target_instance_record = db.getRecordInstance(channel_id);
      ArrayList<String> target_invite = db.getRecordInvite(commandEvent.getAuthor().getIdLong(),
          channel_id);

      if (target_instance_record.get(5).equals("1")) {
        channel.sendMessage("game already started you loser").queue();
        return;
      }

      // if private
      if (target_instance_record.get(4).equals("0")) {
        if (target_invite == null) {
          channel.sendMessage("You do not have invite to this instance").queue();
          return;
        }
      }

      if (Integer.parseInt(target_instance_record.get(2)) == 0) {
        channel.sendMessage("Not enough free spot for this instance ").queue();
        return;
      }

      db.joinInstance(commandEvent.getAuthor().getIdLong(), channel_id);

      // room private->have invite/public. check free slots.

      // if request to join when already in, message you're already in the channel
      // Request to join an instance

      // TODO: Check for permission to join instance

      // Join the channel
      // TODO: Search Database for channel id using name

      TextChannel tc = commandEvent.getGuild().getTextChannelById(channel_id);
      PermissionOverrideAction poa = tc.createPermissionOverride(commandEvent.getMember());
      ArrayList<Permission> permissions = new ArrayList<Permission>();
      permissions.add(Permission.MESSAGE_READ);
      permissions.add(Permission.MESSAGE_WRITE);
      permissions.add(Permission.VIEW_CHANNEL);
      poa.setAllow(permissions);
      poa.queue();
    }
    else if (command.contentEquals("help")) {
      channel.sendMessage(help(arguments)).queue();
    }
    else {
      // Invalid command, commence suggestions?
      int suggested = suggest(command);
      String suggestion = "";
      if(suggested == -1) {
        suggestion = "We couldn't figure out what you were going for there.\nType !help for a list of commands.";
      }else {
        suggestion = "did you mean: "+help(new String[] {commands.get(suggested)[0]});
      }
      channel.sendMessage("Invalid command. "+suggestion);
    }

  }
}
