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

package com.vb.alphapackbot.commands;

import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import com.vb.alphapackbot.Cache;
import com.vb.alphapackbot.Commands;
import com.vb.alphapackbot.RarityTypes;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class OccurrenceCommand extends AbstractCommand {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final RarityTypes requestedRarity;

  public OccurrenceCommand(final List<Message> messages,
                           final GuildMessageReceivedEvent event,
                           final Commands command,
                           final RarityTypes requestedRarity,
                           final Cache cache) {
    super(messages, event, command, cache);
    this.requestedRarity = requestedRarity;
  }

  @Override
  public void run() {
    Optional<Message> result;
    if (command == Commands.FIRST) {
      result = getOccurrence(Lists.reverse(messages));
    } else {
      result = getOccurrence(messages);
    }
    result.ifPresent(this::printOccurrence);
    finish();
  }

  /**
   * Finds first occurrence of rarity.
   *
   * @param messages list of messages in which the rarity is searched
   * @return {@link Optional} of message (empty if specified rarity is not present)
   */
  private Optional<Message> getOccurrence(List<Message> messages) {
    for (Message message : messages) {
      try {
        RarityTypes rarity = loadOrComputeRarity(message);
        if (rarity == requestedRarity) {
          return Optional.of(message);
        }
      } catch (IOException e) {
        log.atSevere().log("Exception getting an image!");
      }
    }
    return Optional.empty();
  }

  /**
   * Prints occurrence to console and sends message to channel if enabled.
   *
   * @param message message of the occurrence
   */
  private void printOccurrence(@NotNull Message message) {
    OffsetDateTime timeCreated = message.getTimeCreated();
    String reply = "You opened your " + command.toString() + " "
        + requestedRarity.toString() + " on "
        + timeCreated.getDayOfMonth() + "." + timeCreated.getMonth().getValue()
        + "." + timeCreated.getYear()
        + " at " + timeCreated.getHour() + ":" + timeCreated.getMinute() + "\n"
        + "link: " + message.getJumpUrl() + ".";

    System.out.println(reply);

    if (properties.isPrintingEnabled()) {
      event.getMessage().reply(reply).complete();
    }
  }
}
