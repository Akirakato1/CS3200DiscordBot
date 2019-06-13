import java.beans.EventSetDescriptor;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.*;
import net.dv8tion.jda.core.events.user.UserTypingEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.restaction.ChannelAction;
public class MyEventListener extends ListenerAdapter{

  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) return;
    
    Message message = event.getMessage();
    String content[]=message.getContentRaw().split(" ");
    MessageChannel channel = event.getChannel();
    TextChannel channelt=event.getTextChannel();
    ChannelManager cm=channelt.getManager();
    
    
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
  }
  
  public void onUserTyping(UserTypingEvent event) {
	  MessageChannel channel = event.getChannel();
	  channel.sendMessage(event.getUser().getName() + " started typing at " + event.getTimestamp()).queue();
  }
}

