import java.util.ArrayList;
import java.util.Arrays;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

// Abstracted functionality for a command manager.
public abstract class CommandManager {
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
}
