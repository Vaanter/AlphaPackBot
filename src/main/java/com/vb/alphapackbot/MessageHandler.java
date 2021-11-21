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
import com.vb.alphapackbot.commands.CountCommand;
import com.vb.alphapackbot.commands.OccurrenceCommand;
import com.vb.alphapackbot.commands.StatusCommand;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Handles received messages and executes requested commands.
 */
@Singleton
public class MessageHandler extends ListenerAdapter {
  private static final String invalidCommandMessage = """
      Invalid command, available commands:\s
      count - Counts all rarities
      last <rarity> - Prints last occurrence of rarity
      first <rarity> - Prints first occurrence of rarity
      status - Prints bot status""";
  private final ExecutorService executor = Executors.newFixedThreadPool(5);
  final Telemetry telemetry;
  final Properties properties;

  @Inject
  MessageHandler(final Properties properties, final Telemetry telemetry) {
    this.properties = properties;
    this.telemetry = telemetry;
  }

  @Override
  public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
    Message message = event.getMessage();
    if (event.getAuthor().isBot() || !properties.isBotEnabled()
        || !message.getContentStripped().toLowerCase(Locale.getDefault()).startsWith("*pack")) {
      return;
    }
    Optional<Commands> command = parseCommand(message.getContentStripped());
    if (command.isEmpty()) {
      if (properties.isPrintingEnabled()) {
        message
            .reply(invalidCommandMessage)
            .complete();
      }
      if (properties.isPrintingEnabled()) {
        message.addReaction("U+1F44E").complete(); // Thumbs down emoji
      }
      return;
    }
    telemetry.getCommandsReceived().increment();
    if (properties.isPrintingEnabled()) {
      message.addReaction("U+1F44D").complete(); // Thumbs up emoji
    }
    if (command.get() == Commands.COUNT) {
      HashSet<User> mentions = new HashSet<>();
      if (!message.getMentionedRoles().isEmpty()) {
        event.getGuild()
            .getMembersWithRoles(message.getMentionedRoles())
            .stream()
            .map(Member::getUser)
            .forEach(mentions::add);
      }
      if (!message.getMentionedUsers().isEmpty()) {
        mentions.addAll(message.getMentionedUsers());
      }
      if (mentions.isEmpty()) {
        mentions.add(event.getAuthor());
      }
      for (User user : mentions) {
        CountCommand countCommand = new CountCommand(
            user.getId(),
            event,
            command.get());
        properties.getProcessingCounter().increment();
        executor.execute(countCommand);
      }
    } else if (command.get() == Commands.STATUS) {
      var statusCommand = new StatusCommand(event, properties, telemetry);
      statusCommand.sendStatus();
    } else {
      Optional<RarityTypes> rarity = parseRarity(message.getContentStripped());
      if (rarity.isEmpty()) {
        if (properties.isPrintingEnabled()) {
          String invalidRarity =
              """
              Invalid rarity, acceptable rarities: Common, Uncommon, Rare, Epic, Legendary, Unknown
              """;
          message
              .reply(invalidRarity)
              .complete();
        }
        return;
      }
      var occurrenceCommand = new OccurrenceCommand(event, command.get(), rarity.get());
      properties.getProcessingCounter().increment();
      executor.execute(occurrenceCommand);
    }
  }

  /**
   * Parses command from second position (indexed from 1) in message.
   * <p>Available commands are specified in {@link Commands}</p>
   *
   * @param message String representation of a message.
   * @return {@link Optional} of {@link Commands} or empty if invalid / none command is passed.
   */
  private Optional<Commands> parseCommand(@NotNull String message) {
    List<String> messageParts = Splitter.on(" ").splitToList(message);
    if (messageParts.size() > 1) {
      return Commands.parse(messageParts.get(1));
    }
    return Optional.empty();
  }

  /**
   * Parses rarity from third position (indexed from 1) in message.
   * <p>Available rarities are specified in {@link RarityTypes}</p>
   *
   * @param message String representation of message.
   * @return {@link Optional} of {@link RarityTypes} or empty if invalid / none rarity is passed.
   */
  private Optional<RarityTypes> parseRarity(@NotNull String message) {
    List<String> messageParts = Splitter.on(" ").splitToList(message);
    if (messageParts.size() > 2) {
      return RarityTypes.parse(messageParts.get(2));
    }
    return Optional.empty();
  }
}
