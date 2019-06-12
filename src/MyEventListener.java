import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.*;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
public class MyEventListener extends ListenerAdapter{

  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) return;
    
    Message message = event.getMessage();
    String content=message.getContentRaw();
    MessageChannel channel = event.getChannel();
    
    if (content.startsWith("!ping")) {
      channel.sendMessage("Pong" + event.getJDA().getPing()).queue();
      
    }
  }
}
