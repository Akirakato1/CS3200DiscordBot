import net.dv8tion.jda.core.*;

public class Main {
  public static void main(String[] args) throws Exception{
    try {
      JDA api = new JDABuilder(AccountType.BOT).setToken("NTg4NDI1Nzg1OTIwOTc4OTY5.XQFB9Q.Oi4thZhK0qPXpH4_Hg9xXqny5Ys").buildAsync();
      api.addEventListener(new MyEventListener());
    } catch (Exception e) {
      e.printStackTrace();
    }
  
  }
}
