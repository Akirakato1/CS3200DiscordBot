import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class QuizBowlManager extends GameManager {
  HashMap<Long, HashMap<String, ScheduledFuture>> globalTimers;
  ScheduledExecutorService executorService;

  private static final int CORRECT_SCORE = 10;
  private static final int FAST_BONUS = 5;
  private static final int SPEECH_RATE = 545;
  private static final int PING_DELAY = 5000;
  private static final int ANSWER_DELAY = 10000;
  private static final int DELAY_BETWEEN_ROUNDS = 2000;

  public QuizBowlManager(Database db) {
    super(db);
  }

  public void update(MessageReceivedEvent message) {
    Long session = message.getChannel().getIdLong();
    // Check if the thread is in the ping stage
    HashMap<String, ScheduledFuture> timers = globalTimers.get(session);
    TextChannel channel = message.getTextChannel();
    if (timers.containsKey("pingTimeUp")) {
      // In the ping stage
    }
    else {
      Long playerID = message.getAuthor().getIdLong();
      if (timers.containsKey("criticalPoint")) {
        // Answering before the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped(), playerID, true);

      }
      else if (timers.containsKey("answerTimeUp")) {
        // Answering after the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped(), playerID, false);
      }
    }
  }

  public boolean canPause(Long sessionID) {
    Set<String> timers = globalTimers.get(sessionID).keySet();
    if (timers.contains("pingTimeUp") || timers.contains("answerTimeUp")) {
      return false;
    }
    return true;
  }

  private void processAnswer(TextChannel channel, Long session, String answer, Long speaker,
      boolean fast) {
    // TODO: Determine if the person speaking is allowed to answer
    if (Long.parseLong(
        db.getInstanceField("QuizBowl_Single", "answering", session).get(0)) == speaker) {
      // Cancel running timers.
      for (ScheduledFuture f : globalTimers.get(session).values()) {
        f.cancel(true);
      }
      globalTimers.get(session).clear();
      // TODO: Judge answer

      // Otherwise ready the next question
      readyNextQuestion(channel, session, DELAY_BETWEEN_ROUNDS); // TODO: Modify with delay so far

    }
  }

  private void readyNextQuestion(TextChannel channel, Long sessionID, long pDelay) {
    
    // Is this the last question?
    // Table, Field, session
    if (db.getInstanceField("QuizBowl_Single", "questions_left", sessionID).get(0).equals("0")) {
      HashMap<Long, Integer> scores = new HashMap<Long, Integer>();
      for (Member m : channel.getMembers()) {
        Long player = m.getUser().getIdLong();

        // Get player score components
        int correct = Integer.parseInt(
            db.getInstancedPlayerField("QuizBowl_Player", "correct_answers", sessionID, player));
        int speedy = Integer.parseInt(
            db.getInstancedPlayerField("QuizBowl_Player", "speed_bonus", sessionID, player));

        // Insert into HashMap
        scores.put(player, CORRECT_SCORE * correct + FAST_BONUS * speedy);
      }
      endGame(sessionID, scores, true);
      return;
    }
    
    
    // Select new question
    ArrayList<String> question = db.getRandomTuple("QuizBowl_Question");
    int qid = Integer.parseInt(question.get(0));

    readyQuestion(channel, question, qid, sessionID, pDelay);

    // Subtract 1 from questions left
    try {
      db.executeUpdate(db.getConnection(),
          "UPDATE QuizBowl_Single SET questions_left = questions_left - 1 WHERE instance_id="
              + sessionID);
    }
    catch (SQLException e) {
      System.out.println("Couldn't update questions left!");
      e.printStackTrace();
    }
  }
  
  // Helper method for both readying a new question and repeating the previous question.
  void readyQuestion(TextChannel channel, ArrayList<String> question, int qid, Long sessionID, Long pDelay) {
    ArrayList<String> qWords = new ArrayList<String>();
    qWords.addAll(Arrays.asList(question.get(1).split(" ")));
    // Process question to find key point
    int keyPoint = qWords.size() - 1; // Default as the last word
    for (int i = 0; i < qWords.size(); i++) {
      String word = qWords.get(i);
      if (word.contains("(*)")) {
        // This is the key point
        keyPoint = i;
        word = word.replace("(*)", "");
      }
      word = word.trim();
      qWords.set(i, word);
    }
    // Remove empty words, if there are any. Go backwards to avoid funny index stuff
    // on deletion.
    for (int i = qWords.size() - 1; i >= 0; i--) {
      if (qWords.get(i).isEmpty()) {
        if (i <= keyPoint) {
          keyPoint--;
        }
        qWords.remove(i);
      }
    }
    // Assemble updates into Futures and apply to the HashMap
    String question_so_far = "";
    long delay = 0;
    for (int i = 0; i < qWords.size(); i++) {
      String word = qWords.get(i);
      question_so_far = question_so_far.concat(word);
      delay += SPEECH_RATE;
      // Apply string to a message
      ScheduledFuture messageSend = channel.sendMessage(question_so_far).queueAfter(delay + pDelay,
          TimeUnit.MILLISECONDS);
      String mName;
      ;
      if (i == keyPoint) {
        mName = "criticalPoint";
      }
      else {
        mName = Integer.toHexString(i);
      }
      globalTimers.get(sessionID).put(mName, messageSend);
    }
    // Prepare time up event
    Runnable timeUp = new Runnable() {
      public void run() {
        pingTimeUp(channel, sessionID);
      }
    };
    ScheduledFuture timeUpFuture = executorService.schedule(timeUp, delay + PING_DELAY,
        TimeUnit.MILLISECONDS);
    // Add to Hash
    globalTimers.get(sessionID).put("answerTimeUp", timeUpFuture);
  }

  // Handles time up for pinging
  void pingTimeUp(TextChannel channel, Long instanceID) {
    int qid = Integer
        .parseInt(db.getInstanceField("QuizBowl_Single", "current_q", instanceID).get(0));
    String answer = "";
    try {
      ResultSet rs = db.getResultSet("SELECT answer FROM QuizBowl_Questions WHERE qbq_id = " + qid);
      rs.next();
      answer = rs.getString(1);
    }
    catch (SQLException e) {
      System.out.println("Couldn't retrieve answer?!");
      e.printStackTrace();
    }

    // Print the answer!
    channel.sendMessage("Time up! the correct answer was: \n" + answer);

    readyNextQuestion(channel, instanceID, DELAY_BETWEEN_ROUNDS);

  }

  // Handles time up for answering
  void answerTimeUp(TextChannel channel, Long instanceID) {

    // Clear answering
    try {
      db.executeUpdate(db.getConnection(),
          "UPDATE QuizBowl_Single SET answering = NULL WHERE instance_id = " + instanceID);
    }
    catch (SQLException e) {
      System.out.println("Couldn't clear answering");
      e.printStackTrace();
    }
  }

  @Override
  void start(String[] arguments, Long instanceID) throws Exception {
    // TODO Auto-generated method stub
    
  }

}