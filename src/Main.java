import net.dv8tion.jda.core.*;

public class Main {
  public static void main(String[] args) throws Exception{
    try {
      JDA api = new JDABuilder(AccountType.BOT).setToken("NTg4NDI1Nzg1OTIwOTc4OTY5.XQFB9Q.Oi4thZhK0qPXpH4_Hg9xXqny5Ys").build();
      api.addEventListener(new MyEventListener());
      Database app = new Database();
      app.run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
