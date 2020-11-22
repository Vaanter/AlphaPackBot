package com.vb.alphapackbot;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import io.micronaut.configuration.picocli.PicocliRunner;
import java.util.Scanner;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import lombok.extern.flogger.Flogger;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Flogger
@Command(name = "AlphaPackBot",
    description = "Bot for counting various types from packs in R6 Siege",
    mixinStandardHelpOptions = true)
public class Main implements Runnable {
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

  @Inject
  MessageHandler messageHandler;
  public static void main(String[] args) {
    PicocliRunner.run(Main.class, args);
  }

  @Override
  public void run() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Thread mainBotThread = new Thread(() -> {
      JDABuilder jda = JDABuilder
          .createLight(token)
          .addEventListeners(messageHandler);
      try {
        mainJda = jda.build();
      } catch (LoginException e) {
        log.atSevere()
            .withCause(e)
            .log("Invalid token!");
      }
    }, "mainBotThread");
    mainBotThread.start();
    try(Scanner scanner = new Scanner(System.in, Charsets.UTF_8)) {
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
