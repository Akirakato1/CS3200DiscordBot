import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class QuizBowlManager extends GameManager {
  HashMap<Long, HashMap<String, ScheduledFuture>> globalTimers;
  ScheduledThreadPoolExecutor executorService;

  private static final int CORRECT_SCORE = 10;
  private static final int FAST_BONUS = 5;
  private static final int SPEECH_RATE = 545;
  private static final int PING_DELAY = 5000;
  private static final int ANSWER_DELAY = 10000;
  private static final int DELAY_BETWEEN_ROUNDS = 2000;
  private static final float SPELLING_THRESHOLD = 0.1f;
  private static final int WORDS_AT_A_TIME = 5;

  public QuizBowlManager(Database db) {
    super(db);
    globalTimers = new HashMap<Long, HashMap<String, ScheduledFuture>>();
    executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    executorService.setRemoveOnCancelPolicy(true);
  }

  public void update(MessageReceivedEvent message) {
    Long session = message.getChannel().getIdLong();
    // Check if the thread is in the ping stage
    HashMap<String, ScheduledFuture> timers = globalTimers.get(session);
    TextChannel channel = message.getTextChannel();
    if (timers.containsKey("pingTimeUp")) {
      // In the ping stage
      if (!timers.get("criticalPoint").isDone()) {
        // Ping before critical point
        ping(channel, message.getMember(), session, true);
      }
      else {
        // Answering after the critical point
        ping(channel, message.getMember(), session, false);

      }
    }
    else {
      Long playerID = message.getAuthor().getIdLong();
      if (timers.containsKey("answerTimeUpFast")) {
        // Answering after the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped(), playerID, true);
      }
      else if (timers.containsKey("answerTimeUpSlow")) {
        // Answering after the critical point has been reached
        processAnswer(channel, session, message.getMessage().getContentStripped(), playerID, false);
      }
    }

  }

  public void ping(TextChannel channel, Member author, Long session, boolean fast) {
    // Check for ping permissions
    int teamID = Integer.parseInt(db.getInstancedPlayerField("QuizBowl_Player", "team_id", session,
        author.getUser().getIdLong()));
    String teamPermission = db.getFieldWithConditionTable("QuizBowl_Team", "buzzable",
        "instance_id=" + session + " AND team_id=" + teamID);
    String personalPermission = db.getInstancedPlayerField("QuizBowl_Player", "buzzable", session,
        author.getUser().getIdLong());
    if (teamPermission.equals("1") && personalPermission.equals("1")) {

      // Clear timers
      HashMap<String, ScheduledFuture> timers = globalTimers.get(session);
      System.out.println("Futures: " + timers.size());
      for (ScheduledFuture future : timers.values()) {
        if (!future.isDone()) {
          if (!future.cancel(true)) {
            System.out.println("Could not cancel future!");
          }
          else {
            System.out.println("Successfully cancelled future");
          }
        }
        else {
          System.out.println("Future was already completed.");
        }
      }
      timers.clear();
      // Set database fields
      db.updateInstanceField("QuizBowl_Single", "answering", "" + author.getUser().getIdLong(),
          session);

      // Add answer time-out Future
      Runnable ato = new Runnable() {
        public void run() {
          answerTimeUp(channel, session);
          readyNextQuestion(channel, session, DELAY_BETWEEN_ROUNDS);
        }
      };
      ScheduledFuture atof = executorService.schedule(ato, ANSWER_DELAY, TimeUnit.MILLISECONDS);
      String tun = "answerTimeUp";
      if (fast) {
        tun = tun.concat("Fast");
      }
      else {
        tun = tun.concat("Slow");
      }
      timers.put(tun, atof);

      // Acknowledge
      channel.sendMessage(author.getEffectiveName() + " buzzed first! You may answer.").queue();
    }
    else {
      // Wrong team
      channel.sendMessage(author.getEffectiveName() + "! You can't buzz, you're on the wrong team!")
          .queue();
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
      int qid = Integer
          .parseInt(db.getInstanceField("QuizBowl_Single", "current_q", session).get(0));
      String correctAnswer = db.getFieldWithConditionTable("QuizBowl_Questions", "answer",
          "qbq_id=" + qid);
      String[] delimiters = { "&", "; accept", "[accept", "prompt" };
      int trimPoint = correctAnswer.length() - 1;
      for (String s : delimiters) {
        int index = correctAnswer.indexOf(s);
        if (index != -1) {
          trimPoint = Math.min(trimPoint, index);
        }
      }
      correctAnswer = correctAnswer.substring(0, trimPoint);
      // Remove other characters
      String regexFilter = "[^a-zA-Z0-9\\-':]";
      correctAnswer.replaceAll(regexFilter, "");
      answer.replaceAll(regexFilter, "");
      answer = answer.trim();
      // Split by "or"
      String[] goodAnswers = correctAnswer.split("or");

      // Compare the given and known answers
      boolean correct = false;
      for (String check : goodAnswers) {
        check = check.trim();
        if (minDistance(check, answer) / answer.length() < SPELLING_THRESHOLD) {
          correct = true;
        }
      }

      // Apply info to database and write response
      String message = "Sorry! Better luck next time!";
      if (correct) {
        message = "Correct! You won " + CORRECT_SCORE + " points!\nThe correct answer was: "+correctAnswer;
        db.updatePlayerInstanceField("QuizBowl_Player", "correct_answers", "correct_answers+1",
            session, speaker);
        if (fast) {
          message = "Impressive! You answered before the power mark, earning you "
              + (CORRECT_SCORE + FAST_BONUS) + " points!\nThe correct answer was: "+correctAnswer;
          db.updatePlayerInstanceField("QuizBowl_Player", "speed_bonus", "speed_bonus+1", session,
              speaker);
        }
      }
      else {
        db.updatePlayerInstanceField("QuizBowl_Player", "missed_answers", "missed_answers+1",
            session, speaker);
      }

      channel.sendMessage(message).queue();

      // Ready question or repeat question
      if (correct) {
        // Ready next question if correct
        readyNextQuestion(channel, session, DELAY_BETWEEN_ROUNDS);
      }
      else {
        // Repeat this question if incorrect

        // Add team buzz restrictions
        int teamID = Integer
            .parseInt(db.getInstancedPlayerField("QuizBowl_Player", "team_id", session, speaker));
        try {
          db.executeUpdate(db.getConnection(),
              "UPDATE QuizBowl_Team SET buzzable=0 WHERE instance_id=" + session + " AND team_id="
                  + teamID);
        }
        catch (SQLException e) {
          System.out.println("Couldn't add team buzz restrictions");
          e.printStackTrace();
        }
        // Add player buzz restrictions
        db.updatePlayerInstanceField("QuizBowl_player", "buzzable", "0", speaker, session);

        // Ready question repetition
        String question = db.getFieldWithConditionTable("QuizBowl_Questions", "question",
            "qbq_id=" + qid);
        readyQuestion(channel, question, qid, session, (long) DELAY_BETWEEN_ROUNDS);
      }

    }
  }

  private void readyNextQuestion(TextChannel channel, Long sessionID, long pDelay) {

    // Is this the last question?
    // Table, Field, session
    if (db.getInstanceField("QuizBowl_Single", "questions_left", sessionID).get(0).equals("0")) {
      HashMap<Long, Integer> scores = calculateScores(channel, sessionID);
      endGame(channel, sessionID, scores, true);
      return;
    }

    System.out.println("updating buzzables");
    // Clear team restrictions on answering
    db.updateInstanceField("QuizBowl_Team", "buzzable", "1", sessionID);
    // Clear user restrictions on answering
    db.updateInstanceField("QuizBowl_Player", "buzzable", "1", sessionID);

    System.out.println(" randomtuple");
    // Select new question
    ArrayList<String> question = db.getRandomTuple("QuizBowl_Questions");
    int qid = Integer.parseInt(question.get(0));

    // Update question to the database
    db.updateInstanceField("QuizBowl_Single", "current_q", "" + qid, sessionID);

    System.out.println("read q");
    readyQuestion(channel, question.get(2), qid, sessionID, pDelay);
  }

  // Helper method for both readying a new question and repeating the previous
  // question.
  void readyQuestion(TextChannel channel, String question, int qid, Long sessionID, Long pDelay) {

    System.out.println("start of ready q");
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

    System.out.println("updated questions left");

    ArrayList<String> qWords = new ArrayList<String>();
    qWords.addAll(Arrays.asList(question.split(" ")));
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

    System.out.println("processed question");
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

    System.out.println("removed empty words");
    long mid = channel.sendMessage("Ready?").complete().getIdLong();

    // Assemble updates into Futures and apply to the HashMap
    String question_so_far = "";
    long delay = 0;
    int expectedSize = 0;
    boolean containsCriticalPoint = false;
    for (int i = 0; i < qWords.size(); i++) {
      String word = qWords.get(i);
      question_so_far = question_so_far.concat(word + " ");
      delay += SPEECH_RATE;
      // Check for critical point
      if (i == keyPoint) {
        containsCriticalPoint = true;
      }
      // Apply string to a message
      if (i % WORDS_AT_A_TIME == 0 || i == qWords.size() - 1) {
        String messageText = question_so_far;
        Runnable smR = new Runnable() {
          public void run() {
            channel.editMessageById(mid, messageText).queue();

          }
        };
        ScheduledFuture messageSend = executorService.schedule(smR, delay + pDelay,
            TimeUnit.MILLISECONDS);
        String mName;
        expectedSize++;
        if (containsCriticalPoint) {
          mName = "criticalPoint";
          containsCriticalPoint = false;
        }
        else {
          mName = Integer.toHexString(i);
        }
        globalTimers.get(sessionID).put(mName, messageSend);
      }
    }
    System.out.println("Expected size: " + expectedSize + 1);
    System.out.println("assemble update into futre hashmap");

    // Prepare time up event
    Runnable timeUp = new Runnable() {
      public void run() {
        pingTimeUp(channel, sessionID);
      }
    };

    System.out.println("prepare time up event");
    ScheduledFuture timeUpFuture = executorService.schedule(timeUp, delay + PING_DELAY,
        TimeUnit.MILLISECONDS);
    // Add to Hash
    globalTimers.get(sessionID).put("pingTimeUp", timeUpFuture);

    System.out.println("Actual size: " + globalTimers.get(sessionID).size());

    System.out.println("finished with readyq");
  }

  // Handles time up for pinging
  void pingTimeUp(TextChannel channel, Long instanceID) {
    System.out.println("Called Ping Time Up");
    int qid = Integer
        .parseInt(db.getInstanceField("QuizBowl_Single", "current_q", instanceID).get(0));
    String answer = "";
    try {
      System.out.println("Getting result set");
      ResultSet rs = db.getResultSet("SELECT answer FROM QuizBowl_Questions WHERE qbq_id = " + qid);
      rs.next();
      answer = rs.getString(1);
      rs.close();
      System.out.println("Got result string");
    }
    catch (SQLException e) {
      System.out.println("Couldn't retrieve answer?!");
      e.printStackTrace();
    }

    // Print the answer!
    channel.sendMessage("Time up! the correct answer was: \n" + answer).queue();
    System.out.println("Correct answer: " + answer);

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
    //TODO: Display the correct answer (remove later)
    String qid = db.getFieldWithConditionTable("QuizBowl_Single", "current_q", "instance_id="+instanceID);
    String correctAnswer = db.getFieldWithConditionTable("QuizBowl_Questions", "answer",
        "qbq_id=" + qid);
    String[] delimiters = { "&", "; accept", "[accept", "prompt" };
    int trimPoint = correctAnswer.length() - 1;
    for (String s : delimiters) {
      int index = correctAnswer.indexOf(s);
      if (index != -1) {
        trimPoint = Math.min(trimPoint, index);
      }
    }
    correctAnswer = correctAnswer.substring(0, trimPoint);
    String message = "You didn't answer quick enough!\nThe correct answer was: "+correctAnswer;
    channel.sendMessage(message).queue();
    
  }

  private HashMap<Long, Integer> calculateScores(TextChannel channel, Long sessionID) {
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
    return scores;
  }

  @Override
  String start(String[] arguments, Long instanceID, TextChannel channel) throws Exception {
    if (arguments.length != 2) {
      throw new Exception("ERROR: Quiz Bowl expects 2 arguments to start");
    }
    // Get the maximum number of questions
    int numberOfQuestions = 0;
    try {
      numberOfQuestions = Integer.parseInt(arguments[0]);
    }
    catch (NumberFormatException e) {
      throw new Exception(
          this.errorMessage("start", arguments, 0, arguments[0] + " is not an integer"));
    }
    if (numberOfQuestions < 1) {
      throw new Exception(
          errorMessage("start", arguments, 0, arguments[0] + " should be at least 1"));
    }

    // Get target score
    int targetScore = 0;
    try {
      targetScore = Integer.parseInt(arguments[1]);
    }
    catch (NumberFormatException e) {
      throw new Exception(
          this.errorMessage("start", arguments, 0, arguments[1] + " is not an integer"));
    }
    if (targetScore < 10) {
      throw new Exception(
          errorMessage("start", arguments, 1, arguments[1] + " should be at least 10"));
    }

    // Create Future hashmap for session
    globalTimers.put(instanceID, new HashMap<String, ScheduledFuture>());

    // Manage and display the teams

    // Get the teams associated with the Instance
    ArrayList<String> teams = db.getInstanceField("Team", "team_id", instanceID);
    ArrayList<String> teamNames = new ArrayList<String>();
    for (String id : teams) {
      teamNames.add(db.getFieldWithConditionTable("Team", "team_name", "team_id=" + id));
    }

    // Get players
    ArrayList<String> playerIDs = db.getInstanceField("Plays", "player_id", instanceID);

    // Add corresponding QuizBowl_Team's
    for (String tID : teams) {
      db.createTuple("QuizBowl_Team", "team_id, instance_id, buzzable",
          tID + "," + instanceID + ", 1");
    }

    // Initialize QuizBowl_Single tuple
    db.createTuple("QuizBowl_Single", "instance_id, questions_left, target_score",
        instanceID + ", " + numberOfQuestions + ", " + targetScore);

    // Initialize QuizBowl_Player's
    for (String pIDS : playerIDs) {
      String tID = db.getInstancedPlayerField("Plays", "team_id", instanceID, Long.parseLong(pIDS));
      db.createTuple("QuizBowl_Player",
          "player_id, instance_id, correct_answers, missed_answers, speed_bonus, buzzable, team_id",
          pIDS + ", " + instanceID + ", 0, 0, 0, 1, " + tID);
    }

    System.out.println("ready next q");
    // Ready first question
    this.readyNextQuestion(channel, instanceID, DELAY_BETWEEN_ROUNDS);

    System.out.println("readied next q");
    // Finish initialization
    return "Welcome to QuizBowl! Let's get started with the first question.";
  }

  @Override
  public int getMinTeams() {
    return 2;
  }

  @Override
  protected void quit(Long instanceID, TextChannel channel, HashMap<Long, Integer> scores) {

    // Delete QuizBowl_Teams
    db.deleteTuple("QuizBowl_Team", "instance_id=" + instanceID);

    // Delete QuizBowl_Single
    db.deleteTuple("QuizBowl_Single", "instance_id=" + instanceID);

    // Delete QuizBowl_Player
    db.deleteTuple("QuizBowl_Player", "instance_id=" + instanceID);

    // TODO: display winning team
    String[] winner = this.getTopTeam(instanceID, scores);
    channel.sendMessage("Team " + winner[1] + " won with " + winner[2] + " points!").queue();

  }

  @Override
  public String[] startArguments() {
    return new String[] {"Max questions", "Target score"};
  }

}