package com.vb.alphapackbot;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main {
  static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final ImmutableSet<String> commands = ImmutableSet.of(
      "exit",
      "toggle-printing",
      "uptime",
      "toggle-caching",
      "toggle-bot",
      "toggle-database");
  private static AtomicReference<JDA> mainJda;

  /**Starts the bot.
   *
   * @param args default main argument
   */
  public static void main(String[] args) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Bot mainBot = new Bot();
    Thread mainBotThread = new Thread(() -> {
      String token = "NjkzNTYxNzM0MzYyODI0Nzc2.Xn-6ag.AZKdPcmHTIAsB4QNebS7Z5ROQkk";
      JDABuilder jda = JDABuilder.createDefault(token);
      jda.addEventListeners(mainBot);
      try {
        mainJda = new AtomicReference<>(jda.build());
      } catch (LoginException e) {
        logger.at(Level.SEVERE)
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
        if (mainBot.isProcessing.get()) {
          System.out.print("Bot is currently processing. Exit? (N/y) ");
          String check = scanner.nextLine();
          if (!check.equalsIgnoreCase("y")) {
            continue;
          }
        }
        mainJda.get().shutdownNow();
        scanner.close();
        System.exit(0);
      } else if (command.equalsIgnoreCase("toggle-printing")) {
        mainBot.isPrintingEnabled = !mainBot.isPrintingEnabled;
        System.out.println("Is printing enabled: " + mainBot.isPrintingEnabled);
      } else if (command.equalsIgnoreCase("uptime")) {
        System.out.println("Uptime: " + stopwatch.elapsed());
      } else if (command.equalsIgnoreCase("toggle-caching")) {
        mainBot.isCachingEnabled = !mainBot.isCachingEnabled;
        System.out.println("Is caching enabled: " + mainBot.isCachingEnabled);
      } else if (command.equalsIgnoreCase("toggle-bot")) {
        mainBot.isBotEnabled = !mainBot.isBotEnabled;
        System.out.println("Is bot enabled: " + mainBot.isBotEnabled);
      } else if (command.equalsIgnoreCase("toggle-database")) {
        mainBot.isDatabaseEnabled.set(!mainBot.isDatabaseEnabled.get());
        System.out.println("Is database enabled: " + mainBot.isDatabaseEnabled.get());
      } else {
        System.out.println("Commands:");
        commands.forEach(System.out::println);
        System.out.println();
      }
    }
  }
}
