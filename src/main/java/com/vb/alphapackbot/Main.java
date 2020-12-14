/*
 *    Copyright 2020 Valentín Bolfík
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vb.alphapackbot;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;


/**
 * Main Class
 */
public class Main {
  // Required for flogger to use sfl4j backend
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
      "toggle-database",
      "set-status <status>");
  private JDA mainJda;

  public static void main(String[] args) {
    Main main = new Main();
    int result = main.initJda();
    if (result > 0) {
      System.exit(result);
    }
    main.commandLoop();
  }

  /** Initializes JDA with token.
   *  Safely asks for bot token and uses this token in JDA builder.
   *  Then attempts to build the JDA.
   * @return 0 if JDA starts normally,
   * 1 if token is not valid,
   * 2 if thread gets interrupted while JDA is connecting.
   */
  public int initJda() {
    char[] token = System.console().readPassword("Enter bot token: ");
    MessageHandler messageHandler = new MessageHandler();
    JDABuilder jda = JDABuilder
        .createLight(String.valueOf(token))
        .addEventListeners(messageHandler);
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
    return 0;
  }

  /** Starts the command loop.
   * Loops until exit command is issued. Processes various commands for bot management.
   */
  public void commandLoop() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
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
        } else if (command.toLowerCase().startsWith("set-status")) {
          List<String> commandParts = Splitter.on(" ")
              .limit(2)
              .omitEmptyStrings()
              .trimResults()
              .splitToList(command);
          if (commandParts.size() > 1) {
            System.out.println(commandParts.get(1));
            mainJda.getPresence().setActivity(Activity.playing(commandParts.get(1)));
          } else {
            System.out.println("Status must not be empty!");
          }
        } else {
          System.out.println("Commands:");
          commands.forEach(System.out::println);
          System.out.println();
        }
      }
    }
    mainJda.shutdownNow();
  }
}
