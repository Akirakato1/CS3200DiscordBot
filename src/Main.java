import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Guild;
public class Main {
  public static void main(String[] args) throws Exception{
    try {
      JDA api = new JDABuilder(AccountType.BOT).setToken("NTg4NDI1Nzg1OTIwOTc4OTY5.XQFB9Q.Oi4thZhK0qPXpH4_Hg9xXqny5Ys").build();
      api.addEventListener(new MyEventListener());
      Database db=new Database("sql9295357","lYzGNS9zs5","sql9.freemysqlhosting.net",3306,"sql9295357");
    
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

