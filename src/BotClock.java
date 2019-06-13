import java.util.ArrayList;
import java.util.Date;

public class BotClock implements Runnable{
	public volatile ArrayList<TimedMessage> queuedMessages;
	public volatile boolean running;
	
	public BotClock() {
		queuedMessages = new ArrayList<TimedMessage>();
		running = true;
	}
	
	public void run() {
		while(running) {
			Date now = new Date();
			// Update the queuedMessages block and send message if necessary.
			if(queuedMessages.get(0).forward(now)) {
				queuedMessages.remove(0);
			}
			// Wait for a little bit (0.1s) before trying again.
			try {
				wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		running = false;
	}
}