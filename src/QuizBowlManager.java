import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class QuizBowlManager extends CommandManager{
  int phase;
  int roundNo;
  
  public QuizBowlManager(Database db) {
    super(db);
  }

  
  public void processCommand(MessageReceivedEvent commandEvent) {
    // TODO Auto-generated method stub
    
  }

}
