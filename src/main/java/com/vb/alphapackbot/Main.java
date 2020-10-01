package com.vb.alphapackbot;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import java.nio.charset.Charset;
import java.util.Scanner;
import javax.security.auth.login.LoginException;
import lombok.extern.flogger.Flogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

@Flogger
public class Main {
  private final Properties properties = Properties.getInstance();
  private final ImmutableSet<String> commands = ImmutableSet.of(
      "exit",
      "status",
      "uptime",
      "toggle-printing",
      "toggle-caching",
      "toggle-bot",
      "toggle-database");
  private JDA mainJda;

  /**
   * Starts the bot.
   *
   * @param args default main argument
   */
  public static void main(String[] args) {
    Main main = new Main();
    main.botManager();
  }

  private void botManager() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Bot mainBot = new Bot();
    Thread mainBotThread = new Thread(() -> {
      String token = "NjkzNTYxNzM0MzYyODI0Nzc2.Xn-6ag.AZKdPcmHTIAsB4QNebS7Z5ROQkk";
      JDABuilder jda = JDABuilder.createDefault(token);
      jda.addEventListeners(mainBot);
      try {
        mainJda = jda.build();
      } catch (LoginException e) {
        log.atSevere()
            .withCause(e)
            .log("Invalid token!");
      }
    }, "mainBotThread");
    mainBotThread.start();
    Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
    //Commands
    while (true) {
      String command = scanner.nextLine();
      if (command.equalsIgnoreCase("exit")) {
        if (properties.getIsProcessing().get()) {
          System.out.print("Bot is currently processing. Exit? (N/y) ");
          String check = scanner.nextLine();
          if (check.equalsIgnoreCase("y")) {
            break;
          }
        }
      } else if (command.equalsIgnoreCase("toggle-printing")) {
        properties.setPrintingEnabled(!properties.isPrintingEnabled());
        System.out.println("Is printing enabled: " + properties.isPrintingEnabled());
      } else if (command.equalsIgnoreCase("uptime")) {
        System.out.println("Uptime: " + stopwatch.elapsed());
      } else if (command.equalsIgnoreCase("toggle-caching")) {
        properties.setCachingEnabled(!properties.isCachingEnabled());
        System.out.println("Is caching enabled: " + properties.isCachingEnabled());
      } else if (command.equalsIgnoreCase("toggle-bot")) {
        properties.setBotEnabled(!properties.isBotEnabled());
        System.out.println("Is bot enabled: " + properties.isBotEnabled());
      } else if (command.equalsIgnoreCase("toggle-database")) {
        properties.getIsDatabaseEnabled().set(!properties.getIsDatabaseEnabled().get());
        System.out.println("Is database enabled: " + properties.getIsDatabaseEnabled().get());
      } else if (command.equalsIgnoreCase("status")) {
        System.out.println(properties.toString());
      } else {
        System.out.println("Commands:");
        commands.forEach(System.out::println);
        System.out.println();
      }
    }
  
    mainJda.shutdownNow();
    scanner.close();
    System.exit(0);
  }
}
