import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Guild;
public class Main {
  public static void main(String[] args) throws Exception{
    try {
      //bot invite:
      //https://discordapp.com/api/oauth2/authorize?client_id=588425785920978969&permissions=8&scope=bot
      JDA api = new JDABuilder(AccountType.BOT).setToken("NTg4NDI1Nzg1OTIwOTc4OTY5.XQFB9Q.Oi4thZhK0qPXpH4_Hg9xXqny5Ys").build();
      //Database db=new Database("sql9295993","NnAbW18udW","sql9.freemysqlhosting.net",3306,"sql9295993");
      Database db=new Database("root","Se1freliance","localhost",3306,"gamebot");
      api.addEventListener(new MyEventListener(db));
      api.awaitReady();
      db.updatePlayers(api);
      List<Guild> guilds=api.getGuilds();
      
      ArrayList<String> gametype_names=db.getFieldFromTable("GameType", "name");
      for(Guild g: guilds) {
       for(String name:gametype_names) {
         createCategory(name,g);
       }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void createCategory(String name,Guild g) {
    if(g.getCategoriesByName(name, true).size()==0) {
      g.getController().createCategory(name).queue();; 
     }
  }
}

