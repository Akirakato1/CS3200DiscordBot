import java.util.Date;

import net.dv8tion.jda.core.entities.MessageChannel;

public class TimedMessage {
	MessageChannel channel;
	String messageId;
	String message;
	Date sendTime;
	
	// Constructor for modifying an existing message.
	public TimedMessage(MessageChannel channel, String messageId, String message, Date sendTime) {
		this.channel = channel;
		this.messageId = messageId;
		this.message = message;
		this.sendTime = sendTime;
	}
	
	// Constructor for sending a new message.
	public TimedMessage(MessageChannel channel, String message, Date sendTime) {
		this(channel, null, message, sendTime);
	}
	
	// Checks if the designated time has passed, but does not send the message.
	public boolean peek(Date now) {
		return now.after(sendTime);
	}
	
	// Sends the message if the time has passed.
	public boolean forward(Date now) {
		if(peek(now)) {
			boolean sent = false;
			if(messageId != null) {
				if(!messageId.isEmpty()) {
					// Message id is not null and is not empty. Modify existing message.
					channel.editMessageById(messageId, message).queue();
					sent = true;
				}
			}
			if(!sent) {
				channel.sendMessage(message).queue();
			}
			return true;
		}
		return false;
	}
}
