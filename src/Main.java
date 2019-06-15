import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Guild;
public class Main {
  public static void main(String[] args) throws Exception{
    try {
      
      JDA api = new JDABuilder(AccountType.BOT).setToken("NTg4NDI1Nzg1OTIwOTc4OTY5.XQFB9Q.Oi4thZhK0qPXpH4_Hg9xXqny5Ys").build();
      Database db=new Database("sql9295567","feYphdLwPa","sql9.freemysqlhosting.net",3306,"sql9295567");
      api.addEventListener(new MyEventListener(db));
      api.awaitReady();
      db.updatePlayers(api);
      List<Guild> guilds=api.getGuilds();
      
      for(Guild g: guilds) {
       createCategory("Testing Channels",g);
       createCategory("Quiz Bowl",g);
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

