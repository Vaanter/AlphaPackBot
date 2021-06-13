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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Handles received messages and executes requested commands.
 */
@Singleton
public class MessageHandler extends ListenerAdapter {
  private static final String invalidCommandMessage = "\nInvalid command, available commands: \n"
      + "count - Counts all rarities\n"
      + "last <rarity> - Prints last occurrence of rarity\n"
      + "first <rarity> - Prints first occurrence of rarity\n"
      + "status - Prints bot status";
  private static final String invalidRarity = "\n Invalid rarity, acceptable rarities: "
      + "Common, Uncommon, Rare, Epic, Legendary, Unknown";
  private static final Properties properties = Properties.getInstance();
  final Telemetry telemetry;
  final Cache cache;
  final TypingManager typingManager;
  private final ExecutorService executor = Executors.newFixedThreadPool(5);

  @Inject
  MessageHandler(final Telemetry telemetry,
                 final Cache cache,
                 final TypingManager typingManager) {
    this.telemetry = telemetry;
    this.cache = cache;
    this.typingManager = typingManager;
  }

  @Override
  public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    if (event.getMessage().getContentStripped().toLowerCase(Locale.getDefault()).startsWith("*pack")
        && properties.isBotEnabled()) {
      Optional<Commands> command = parseCommand(event.getMessage().getContentStripped());
      if (command.isEmpty()) {
        if (properties.isPrintingEnabled()) {
          event.getMessage()
              .reply(invalidCommandMessage)
              .complete();
        }
        if (properties.isPrintingEnabled()) {
          event.getMessage().addReaction("U+1F44E").complete();
        }
        return;
      }
      telemetry.getCommandsReceived().increment();
      if (properties.isPrintingEnabled()) {
        event.getMessage().addReaction("U+1F44D").complete();
      }
      if (command.get() == Commands.COUNT) {
        HashSet<User> mentions = new HashSet<>();
        if (!event.getMessage().getMentionedRoles().isEmpty()) {
          event.getGuild()
              .getMembersWithRoles(event.getMessage().getMentionedRoles())
              .stream()
              .map(Member::getUser)
              .forEach(mentions::add);
        }
        if (!event.getMessage().getMentionedUsers().isEmpty()) {
          mentions.addAll(event.getMessage().getMentionedUsers());
        }
        if (mentions.isEmpty()) {
          mentions.add(event.getAuthor());
        }
        for (int i = 0; i < mentions.size(); i++) {
          CountCommand countCommand = new CountCommand(event, command.get(), cache, typingManager);
          properties.getProcessingCounter().increment();
          executor.execute(countCommand);
        }
      } else if (command.get() == Commands.STATUS) {
        StatusCommand statusCommand = new StatusCommand(event, properties, telemetry);
        statusCommand.sendStatus();
      } else {
        Optional<RarityTypes> rarity = parseRarity(event.getMessage().getContentStripped());
        if (rarity.isEmpty()) {
          if (properties.isPrintingEnabled()) {
            event.getMessage()
                .reply(invalidRarity)
                .complete();
          }
          return;
        }
        OccurrenceCommand occurrenceCommand =
            new OccurrenceCommand(event, command.get(), rarity.get(), cache, typingManager);
        properties.getProcessingCounter().increment();
        executor.execute(occurrenceCommand);
      }
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
