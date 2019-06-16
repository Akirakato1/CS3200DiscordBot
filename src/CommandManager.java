import java.util.ArrayList;
import java.util.Arrays;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

// Abstracted functionality for a command manager.
public abstract class CommandManager {
  protected static final float SIMILARITY_THRESHOLD = 0.25f;
  ArrayList<String[]> commands;
  Database db;

  public CommandManager(Database db) {
    this.commands = new ArrayList<String[]>();
    this.db = db;
  }
  
  public abstract void processCommand(MessageReceivedEvent commandEvent);
  
  protected String errorMessage(String command, String[] arguments, int invalid, String description) {
    String message = "Error in !" + command + ": ";
    for (int i = 0; i < arguments.length; i++) {
      if (i > 0) {
        message = message.concat(", ");
      }
      if (i == invalid) {
        message = message.concat("**");
      }
      message = message.concat(arguments[i]);
      if (i == invalid) {
        message = message.concat("**");
      }
    }
    message = message.concat("\n" + description);

    return message;
  }
  
  // Returns a String representing the help function.
  // When given an empty set of arguments, it lists all functions.
  // When given an argument that is a function, it lists the arguments.
  protected String help(String[] arguments) {
    String message = "";
    if(arguments.length > 0) {
      System.out.println(arguments[0]);
      for (String[] function : commands) {
        if (arguments[0].equals(function[0]) || arguments[0].equals("!" + function[0])) {
          message = "!" + function[0] + ": ";
          if (function.length == 1) {
            message = message.concat("This function has no arguments");
          }
          else {
            for (int i = 1; i < function.length; i++) {
              if (i > 1) {
                message = message.concat(", ");
              }
              message = message.concat(function[i]);
            }
          }
        }
      }
    }
    if(message.isEmpty()) {
      for (String[] function : commands) {
        message += ", !" + function[0];
      }
      message = message.substring(2);
    }
    return message;
  }
  
  public void sendPrivateMessage(User user, String content) {
    // openPrivateChannel provides a RestAction<PrivateChannel>
    // which means it supplies you with the resulting channel
    user.openPrivateChannel().queue((channel) -> {
      channel.sendMessage(content).queue();
    });
  }
  
  protected String sanitize(String dirty) {
    dirty = dirty.replaceAll("[^a-zA-Z0-9äöüÄÖÜß?!$§. -]", "");
    dirty.replaceAll(" ", "-");
    return dirty.trim();
  }
  
  protected String[] getArguments(String content, TextChannel channel, String command) {
    // Put arguments in an ArrayList
    ArrayList<String> argumentsList = new ArrayList<String>();
    if(content.length() - command.length() > 2) {
       String[] arrArgs = content.substring(command.length() + 2).trim().split(",");
       argumentsList.addAll(Arrays.asList(arrArgs));
    }
    // Strip whitespace from argument sides
    for (int i = 0; i < argumentsList.size(); i++) {
      argumentsList.set(i, argumentsList.get(i).trim());
    }
    // Remove empty arguments
    int emptyArguments = 0;
    while(argumentsList.contains("")) {
      argumentsList.remove("");
      emptyArguments++;
    }
    // Warn of empty arguments except in special case
    if(emptyArguments > 0 && !(emptyArguments == 1 && argumentsList.size()==0)) {
      channel.sendMessage("Warning: "+emptyArguments+" empty arguments");
    }
    return argumentsList.toArray(new String[0]);
  }
  
  //Super basic command troubleshooting.
  protected int suggest(String command) {
    ArrayList<Float> similarity = new ArrayList<Float>();
    float mostSimilar = -1;
    int similarCommand = -1;
    for(int i = 0; i < commands.size(); i++) {
      String[] match = commands.get(i);
      // Find the similarity index
      similarity.add(minDistance(command, match[0]) / (float)match[0].length());
      if(similarity.get(i) > mostSimilar) {
        // Update most similar
        mostSimilar = similarity.get(i);
        similarCommand = i;
      }
    }
    if(mostSimilar < SIMILARITY_THRESHOLD) {
      return similarCommand;
    }
    return -1;
  }
  
  // String distance function, courtesy of Program Creek
  public static int minDistance(String word1, String word2) {
    int len1 = word1.length();
    int len2 = word2.length();
   
    // len1+1, len2+1, because finally return dp[len1][len2]
    int[][] dp = new int[len1 + 1][len2 + 1];
   
    for (int i = 0; i <= len1; i++) {
      dp[i][0] = i;
    }
   
    for (int j = 0; j <= len2; j++) {
      dp[0][j] = j;
    }
   
    //iterate though, and check last char
    for (int i = 0; i < len1; i++) {
      char c1 = word1.charAt(i);
      for (int j = 0; j < len2; j++) {
        char c2 = word2.charAt(j);
   
        //if last two chars equal
        if (c1 == c2) {
          //update dp value for +1 length
          dp[i + 1][j + 1] = dp[i][j];
        } else {
          int replace = dp[i][j] + 1;
          int insert = dp[i][j + 1] + 1;
          int delete = dp[i + 1][j] + 1;
   
          int min = replace > insert ? insert : replace;
          min = delete > min ? min : delete;
          dp[i + 1][j + 1] = min;
        }
      }
    }
   
    return dp[len1][len2];
  }
}
