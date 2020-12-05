package com.vb.alphapackbot;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import java.util.Scanner;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "AlphaPackBot",
    description = "Bot for counting various types from packs in R6 Siege",
    mixinStandardHelpOptions = true)
public class Main implements Runnable {
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

  @Parameters(index = "0", description = "Token used for Bot initialisation", arity = "1")
  String token;

  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }

  @Override
  public void run() {
    MessageHandler messageHandler = new MessageHandler();
    Stopwatch stopwatch = Stopwatch.createStarted();
    Thread mainBotThread = new Thread(() -> {
      JDABuilder jda = JDABuilder
          .createLight(token)
          .addEventListeners(messageHandler);
      try {
        mainJda = jda.build();
        mainJda.awaitReady();
      } catch (LoginException e) {
        log.atSevere()
            .withCause(e)
            .log("Invalid token!");
        System.exit(1);
      } catch (InterruptedException e) {
        log.atSevere()
            .withCause(e)
            .log("Interrupted while getting ready!");
        System.exit(2);
      }
    }, "mainBotThread");
    mainBotThread.start();
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
        } else {
          System.out.println("Commands:");
          commands.forEach(System.out::println);
          System.out.println();
        }
      }
    }
    mainJda.shutdownNow();
    System.exit(0);
  }
}
