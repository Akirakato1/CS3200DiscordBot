import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
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
    commands.add(new String[] { "help", "context(optional)" });
    commands.add(new String[] { "invite", "[\\@recipient(s)]" });
    commands.add(new String[] { "leave" });
    commands.add(new String[] { "start" });
    
    commands.add(new String[] { "createteam","teamname"});
    commands.add(new String[] { "teams" });
    commands.add(new String[] { "leaveteam" });
    commands.add(new String[] {"jointeam","teamname"});
    commands.add(new String[] { "autoassign" });
    
    // Initialize GameManagers
    gameManagers = new ArrayList<GameManager>();
    gameManagers.add(new QuizBowlManager(db));
    gameManagers.add(null);
    gameManagers.add(new DOneHundredManager(db));
    gameThreads = new HashMap<Long, Integer>();
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
        sendPrivateMessage(m.getUser(), senderName + " invited you to join " + channelName
            + " in Server " + commandEvent.getGuild().getName());
        db.createInvite(commandEvent.getAuthor().getIdLong(), m.getUser().getIdLong(),
            channel.getIdLong());
      }
    }
    else if (command.equals("teams")) {
      ArrayList<Integer> team_ids=db.getTeamIDInstance(channel.getIdLong());
      if(team_ids==null) {
        channel.sendMessage("No teams in this instance yet").queue();
        return;
      }
      String message="";
      for(int team_id:team_ids) {
        String team_name=db.getFieldWithConditionTable("Team", "team_name", "team_id="+team_id);
        message=message+"\n Team **"+team_name+"** \n";
        ArrayList<Long> player_ids=db.getTeamPlayerID(team_id);
        if(player_ids!=null) {
        for(long player_id:player_ids) {
          message=message+commandEvent.getGuild().getMemberById(player_id).getUser().getName()+"  ";
        }
        }
      }
      
      channel.sendMessage(message).queue();
      return;
    }
    else if(command.equals("createteam")) {
      if(arguments.length!=1) {
        channel.sendMessage("invalid number of arguments. need team name").queue();
        return;
      }
      if(db.getTeamIDbyName(channel.getIdLong(), arguments[0])!=null) {
        channel.sendMessage("Error Team name already exists").queue();
        return;
      }
      channel.sendMessage("Created team: "+arguments[0]).queue();
      db.createTeam(channel.getIdLong(), arguments[0]);
    }
    else if (command.equals("leaveteam")) {
      String team_id=db.getFieldWithConditionTable("Plays", "team_id", 
          "player_id="+commandEvent.getAuthor().getIdLong()+" and instance_id="+channel.getIdLong());
      if(team_id==null) {
        channel.sendMessage("You're not in a team").queue();
        return;
      }
      
      db.updatePlaysTeamID(channel.getIdLong(), commandEvent.getAuthor().getIdLong(), null);
      channel.sendMessage("bailed from team successfully").queue();
      return;
    }
    else if (command.equals("jointeam")) {
      if(arguments.length!=1) {
        channel.sendMessage("invalid number of arguments. need team name").queue();
        return;
      }
      Integer team_to_join=db.getTeamIDbyName(channel.getIdLong(), arguments[0]);
      if(team_to_join==null) {
        channel.sendMessage("The team does not exist").queue();
        return;
      }
      
      db.setTeamIDPlays(channel.getIdLong(), commandEvent.getAuthor().getIdLong(), team_to_join);
      
      channel.sendMessage("joined team: "+arguments[0]).queue();
      return;
    }
    else if (command.equals("autoassign")) {
      ArrayList<Integer> team_ids=db.getTeamIDInstance(channel.getIdLong());
      ArrayList<Long> all_players=db.getAllPlayerInstance(channel.getIdLong());
      ArrayList<Long> players_need_assign=new ArrayList<Long>();
      ArrayList<Long> players_with_team=new ArrayList<Long>();
      
      //System.out.println(team_ids);
      //System.out.println(all_players);
      
      if(team_ids==null) {
        channel.sendMessage("No teams to autoassign to").queue();
        return;
      }
      //System.out.println("first team member: "+db.getTeamPlayerID(team_ids.get(0)));
      //System.out.println("second team member: "+db.getTeamPlayerID(team_ids.get(1)));
      
      ArrayList<Integer> num_players=new ArrayList<Integer>();
      for(int team_id:team_ids) {
        System.out.print(team_id+" ");
        ArrayList<Long> player_ids=db.getTeamPlayerID(team_id);
        System.out.print(player_ids+" ");
        if(player_ids==null) {
          num_players.add(0);
        }else {
          players_with_team.addAll(player_ids);
          num_players.add(player_ids.size());
        }
        System.out.println(num_players);
      }
      
      System.out.println(num_players);
      
      int num_of_teams=team_ids.size();
     int total_players= db.getNumMembers(channel.getIdLong());
     System.out.println("total_players: "+total_players);
     int players_in_each_team=(int) Math.ceil(total_players/num_of_teams);
     System.out.println("players_in_each_team: "+players_in_each_team);
     for(int i=0;i<team_ids.size();i++) {
       num_players.set(i,players_in_each_team-num_players.get(i));
     }
     
    // System.out.println("num_player_still_need_join: "+num_players);
     
     for(long player_id:all_players) {
       if(!players_with_team.contains(player_id)) {
         players_need_assign.add(player_id);
       }
     }
     
     //System.out.println("Plyares still need join: "+players_need_assign);
     int shift=0;
     for(int i=0;i<players_need_assign.size();i++) {
       for(int j=0;j<team_ids.size();j++) {
       if(num_players.get((i+shift)%num_of_teams)<1) {
         shift++;
       }else {
         num_players.set((i+shift)%num_of_teams,num_players.get((i+shift)%num_of_teams)-1);
         db.setTeamIDPlays(channel.getIdLong(), players_need_assign.get(i), team_ids.get((i+shift)%num_of_teams));
         break;
       }
       }
     }
     
     channel.sendMessage("AutoAssign complete").queue();
     
      return;
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

        String freespots = db.getInstanceField("Instance", "free_spots", channel.getIdLong())
            .get(0);
        
        if (Integer.parseInt(freespots) < 0) {
          freespots = "Infinite";
        }

        channel.sendMessage(commandEvent.getAuthor().getName()
            + " has left your struggle. \nRemaining free spots: " + freespots).queue();

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
      Long instanceID = commandEvent.getChannel().getIdLong();
      gameThreads.put(instanceID,
          Integer.parseInt(db.getInstanceField("Instance", "game_id", instanceID).get(0)) - 1);
      GameManager gm = gameManagers.get(gameThreads.get(instanceID));
      try {
        String startMessage = gm.start(arguments, instanceID, channel);
        System.out.println("From instance"+startMessage);
        channel.sendMessage(startMessage).queue();
        db.updateInstanceField("Instance", "started", "1", instanceID);
      }
      catch (Exception e) {
        e.printStackTrace();
        String message = e.getMessage();
        channel.sendMessage(message).queue();
        return;
      }
    }
    else if (command.equals("help")) {
      if(arguments.length > 0) {
        if(arguments[0].equals("start") || arguments[0].equals("!start")) {
          // Asking for help on start!
          // Get the appropriate game manager
          int gameID = Integer
              .parseInt(db.getIDbyNameGameType(channel.getParent().getName()));
          System.out.println(gameID);
          String[] sargs = gameManagers.get(gameID-1).startArguments();
          String message="!start: ";
          if(sargs.length == 0) {
            message = message+"This method has no arguments.";
          }else {
            for (int i = 0; i < sargs.length; i++) {
              if (i > 0) {
                message = message.concat(", ");
              }
              message = message.concat(sargs[i]);
            }
          }
          channel.sendMessage(message).queue();
          return;
        }
      }
      channel.sendMessage(help(arguments)).queue();
    }
    else {
      int suggested = suggest(command);
      String suggestion = "";
      if (suggested == -1) {
        suggestion = "We couldn't figure out what you were going for there.\nType !help for a list of commands.";
      }
      else {
        suggestion = "did you mean: " + help(new String[] { commands.get(suggested)[0] });
      }
      channel.sendMessage("Invalid command. " + suggestion).queue();
    }
  }

  // Redirects to the GameManager if applicable, returns if the game has started.
  public boolean instanceMessage(MessageReceivedEvent event) {
    long key = event.getChannel().getIdLong();
    if (!gameThreads.isEmpty()) {
      if (gameThreads.containsKey(key)) {
        // It's a game event! Process immediately!
        gameManagers.get(gameThreads.get(key)).processCommand(event);

        // Check if the game is over
        if (gameManagers.get(gameThreads.get(key)).checkAndApplyFinished(key)) {
          // Remove from cache
          gameThreads.remove(key);
          // TODO: set gameStarted to false on db
          db.updateInstanceField("Instance", "started", "0", event.getChannel().getIdLong());
        }
        return true;
      }
    }

    // Not sure if the instance has started. Database query for it.
    boolean started = db.getInstanceStarted(event.getChannel().getIdLong());
    if (started) {
      // Game is started. Add the missing information to the cache.
      int gameID = Integer
          .parseInt(db.getIDbyNameGameType(event.getTextChannel().getParent().getName()));
      gameThreads.put(key, gameID - 1);
      gameManagers.get(gameThreads.get(key)).processCommand(event);
      try {

        gameManagers.get(gameThreads.get(key)).start(new String[] {}, key, event.getTextChannel());
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    return started;
  }

}
