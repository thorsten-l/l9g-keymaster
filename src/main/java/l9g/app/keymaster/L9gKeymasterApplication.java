package l9g.app.keymaster;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.shell.jline.PromptProvider;

@SpringBootApplication
@CommandScan
public class L9gKeymasterApplication
{

  @Bean
  public PromptProvider createPromptProvider()
  {
    return () -> new AttributedString("keymaster:>",
      AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
  }
  
  public static void main(String[] args)
  {
    SpringApplication.run(L9gKeymasterApplication.class, args);
  }

}
