import java.util.List;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

// Handles commands from Instance threads.
public class InstanceCommandManager {
	
	//Process a command. Assume the command begins with the proper delimiter.
	public void processCommand(MessageReceivedEvent commandEvent) {
		String[] words = commandEvent.getMessage().getContentStripped().split(" ");
		// Determine the command.
		String command = words[0].substring(1);
		
		
		if(command.equals("invite")) {
			// Invite command.
			List<Member> invited = commandEvent.getMessage().getMentionedMembers();
			String senderName = commandEvent.getAuthor().getName();
			String channelName = commandEvent.getChannel().getName();
			for(Member m : invited) {
				//TODO: Add the invite to the SQL database.
				sendPrivateMessage(m.getUser(), senderName + " invited you to join "+channelName);
			}
		} else if(command.equals("leave")) {
			// Leave the instance
		}
		
		
	}
	
	public void sendPrivateMessage(User user, String content) {
        // openPrivateChannel provides a RestAction<PrivateChannel>
        // which means it supplies you with the resulting channel
        user.openPrivateChannel().queue((channel) ->
        {
            channel.sendMessage(content).queue();
        });
    }
}
