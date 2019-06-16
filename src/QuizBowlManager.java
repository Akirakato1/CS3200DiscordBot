import java.util.HashMap;
import java.util.concurrent.Future;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class QuizBowlManager extends GameManager {
  HashMap<Long, HashMap<String, Future>> globalTimers;

  public QuizBowlManager(Database db) {
    super(db);
  }

  public void update(MessageReceivedEvent message) {
    Long session = message.getChannel().getIdLong();
    // Check if the thread is in the ping stage
    HashMap<String, Future> timers = globalTimers.get(session);
    TextChannel channel = message.getTextChannel();
    if (timers.containsKey("pingTimeUp")) {
      // In the ping stage

    }
    else {
      if (timers.containsKey("criticalPoint")) {
        // Answering before the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped());

      }
      else if (timers.containsKey("answerTimeUp")) {
        // Answering after the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped());
      }
    }
  }

  private void processAnswer(TextChannel channel, Long session, String answer) {
    // TODO: Determine if the person speaking is allowed to answer
    if (true) {
      // Clear running timers.
      for(Future f: globalTimers.get(session).values()) {
        f.cancel(false);
      }
      globalTimers.get(session).clear();
      // TODO: Process answer
      
      
    }
  }

}