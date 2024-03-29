
import java.sql.SQLException;
import java.util.ArrayList;


import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.*;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
public class MyEventListener extends ListenerAdapter {
  GeneralCommandManager gcm;
  InstanceCommandManager icm;
  Database db;

  public MyEventListener(Database db) {
    this.gcm = new GeneralCommandManager(db);
    this.icm = new InstanceCommandManager(db);
    this.db = db;
  }

  public void onGuildJoin(GuildJoinEvent event) {
      TextChannel general_channel=event.getGuild().getTextChannelsByName("general", true).get(0);
      general_channel.sendMessage("Hello, I'm the Game-Bot. Please use command **!initbot** to initialise me. ").queue();
    }
  
  public void onGuildMemberJoin(GuildMemberJoinEvent event) {
    try {
      this.db.initPlayer(event.getUser());
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().isBot())
      return;

    Message message = event.getMessage();
    String content[] = message.getContentRaw().split(" ");
   // MessageChannel channel = event.getChannel();
    //TextChannel channelt = event.getTextChannel();
   // ChannelManager cm = channelt.getManager();
    ArrayList<String> gametype_names=db.getFieldFromTable("GameType", "name");

    // Assume it is a command, redirect to appropriate command manager.
    String category = event.getTextChannel().getParent().getName();
    //System.out.println(category);
    if (gametype_names.contains(category)) {
      // It is an instance message.
      
      // Send it to the game processor
      if(!icm.instanceMessage(event) && content[0].startsWith("!")) {
      //Process it as a command if applicable
        System.out.println("Redirecting to instance");
        icm.processCommand(event);
      }
    }
    else {
      if (content[0].startsWith("!")) {
        // It is a general command
        gcm.processCommand(event);
      }
    }

    /*
    if (content[0].equals("!ping")) {
      channel.sendMessage("Pong" + event.getJDA().getPing()+channel.getId()).queue(); 
    }
    
    if (content[0].equals("!getservers")) {
      List<Guild> guilds=message.getJDA().getGuilds();
      String guildnames="";
      for(Guild g:guilds) {
        guildnames=guildnames+g.getName()+" ";
      }
      channel.sendMessage("Server names: "+guildnames).queue();
    }
    if (content[0].equals("!channelnames")) {
      List<Channel> channels=event.getGuild().getChannels();
      
      String channelnames="";
      for(Channel c:channels) {
        channelnames=channelnames+c.getName()+" \n";
      }
      channel.sendMessage("Channel names: "+channelnames).queue();
    }
    if (content[0].equals("!createchannel")) {
      String channelname=content[1];
      for(int i=2;i<content.length;i++) {
        channelname=channelname+"-"+content[i];
      }
      
      String parent="Testing Channels";
      
      List<Channel> channels=event.getGuild().getChannels();
      
      boolean nameused=false;
      
      for(Channel c:channels) {
          if(channelname.equalsIgnoreCase(c.getName())) {
            nameused=true;
            break;
        }
      }
      
      
      if(!nameused) {
      ChannelAction channelaction=event.getGuild().getController().createTextChannel(channelname);
      channelaction.setTopic("Testing Channels");
      channelaction.setParent(event.getGuild().getCategoriesByName(parent, true).get(0));
      channelaction.queue();}
      else{
        channel.sendMessage("name already used").queue();
      }
      
      ChannelManager channelmanager;
      
      for(Channel c:channels) {
        if(c.getName().equals(channelname)) {
          channelmanager=channels.get(0).getManager();
        }
      }
      
    }
    */
  }
/*
  public void onUserTyping(UserTypingEvent event) {
    MessageChannel channel = event.getChannel();
    channel.sendMessage(event.getUser().getName() + " started typing at " + event.getTimestamp())
        .queue();
  }*/
}
