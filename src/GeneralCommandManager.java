import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

// Handles commands from the General thread(s).
public class GeneralCommandManager {
	
	//Process a command. Assume the command begins with the proper delimiter.
	public void processCommand(MessageReceivedEvent commandEvent) {
		String[] words = commandEvent.getMessage().getContentStripped().split(" ");
		// Determine the command.
		String command = words[0].substring(1);
		
		
		if(command.equals("createInstance")) {
			// Create an instance
			
		}
	}
}
