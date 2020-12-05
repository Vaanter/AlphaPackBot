package com.vb.alphapackbot;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class Main {
  static {
    System.setProperty(
        "flogger.backend_factory", "com.google.common.flogger.backend.slf4j.Slf4jBackendFactory#getInstance");
  }

  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final Properties properties = Properties.getInstance();
  private final ImmutableSet<String> commands = ImmutableSet.of(
      "exit",
      "status",
      "uptime",
      "toggle-bot",
      "toggle-database");
  private JDA mainJda;

  public static void main(String[] args) {
    Main main = new Main();
    System.exit(main.start());
  }

  public int start() {
    char[] token = System.console().readPassword("Enter bot token: ");
    MessageHandler messageHandler = new MessageHandler();
    Stopwatch stopwatch = Stopwatch.createStarted();
//    Thread mainBotThread = new Thread(() -> {
    JDABuilder jda = JDABuilder
        .createLight(String.valueOf(token))
        .addEventListeners(messageHandler);
    jda.setActivity(Activity.playing(""));
    Arrays.fill(token, ' ');
    try {
      mainJda = jda.build();
      mainJda.awaitReady();
    } catch (LoginException e) {
      log.atSevere()
          .withCause(e)
          .log("Invalid token!");
      return 1;
    } catch (InterruptedException e) {
      log.atSevere()
          .withCause(e)
          .log("Interrupted while getting ready!");
      return 2;
    }
    try (Scanner scanner = new Scanner(System.in, Charsets.UTF_8)) {
      //Commands
      while (true) {
        String command = scanner.nextLine();
        if (command.equalsIgnoreCase("exit")) {
          synchronized (properties.getProcessingCounter()) {
            if (properties.getProcessingCounter().longValue() > 0) {
              System.out.print("Bot is currently processing. Exit? (N/y) ");
              String check = scanner.nextLine();
              if (!check.equalsIgnoreCase("y")) {
                continue;
              }
            }
            break;
          }
        } else if (command.equalsIgnoreCase("toggle-printing")) {
          properties.setPrintingEnabled(!properties.isPrintingEnabled());
          System.out.println("Is printing enabled: " + properties.isPrintingEnabled());
        } else if (command.equalsIgnoreCase("uptime")) {
          System.out.println("Uptime: " + stopwatch.elapsed());
        } else if (command.equalsIgnoreCase("toggle-bot")) {
          properties.setBotEnabled(!properties.isBotEnabled());
          System.out.println("Is bot enabled: " + properties.isBotEnabled());
        } else if (command.equalsIgnoreCase("toggle-database")) {
          properties.setDatabaseEnabled(!properties.isDatabaseEnabled());
          System.out.println("Is database enabled: " + properties.isDatabaseEnabled());
        } else if (command.equalsIgnoreCase("status")) {
          System.out.println(properties.toString());
        } else if (command.equalsIgnoreCase("set-status")) {
          List<String> commandParts = Splitter.on(" ")
              .limit(2)
              .omitEmptyStrings()
              .trimResults()
              .splitToList(command);
          if (commandParts.size() > 2) {
            mainJda.getPresence().setActivity(Activity.listening(commandParts.get(1)));
          }
        } else {
          System.out.println("Commands:");
          commands.forEach(System.out::println);
          System.out.println();
        }
      }
    }
    mainJda.shutdownNow();
    return 0;
  }
}
