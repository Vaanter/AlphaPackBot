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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.vb.alphapackbot.CommandService;
import com.vb.alphapackbot.RarityTypes;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;
import net.dv8tion.jda.api.entities.Message;

public abstract sealed class OccurenceCommand extends Command
    permits FirstOccurrenceCommand, LastOccurrenceCommand {

  protected enum Type {
    FIRST("first"),
    LAST("last");

    private final String type;

    Type(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  /**
   * Sends a reply with the occurance of requested rarity.
   *
   * @param requestMessage message to reply to
   * @param occuranceMessage message of the occurrence
   * @param requestedRarity rarity specified in the request
   */
  protected void replyOccurrence(
      Message requestMessage, Message occuranceMessage, RarityTypes requestedRarity, Type type) {
    OffsetDateTime timeCreated = occuranceMessage.getTimeCreated();
    String date = timeCreated.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    String time = timeCreated.format(DateTimeFormatter.ofPattern("HH:mm"));

    String reply =
        String.format(
            "You opened your %s %s on %s at %s\n link: %s.",
            type.getType(), requestedRarity, date, time, occuranceMessage.getJumpUrl());

    requestMessage.reply(reply).queue(message -> message.suppressEmbeds(true).queue());
  }

  /**
   * Sends a reply informing that specified rarity has not been found.
   *
   * @param requestMessage original message of the request
   * @param requestedRarity rarity specified in the request
   */
  protected void replyNotFound(Message requestMessage, RarityTypes requestedRarity) {
    String reply = "You have never opened " + requestedRarity.toString() + "!";
    requestMessage.reply(reply).queue();
  }

  protected void doCommand(Type type, CommandEvent event, CommandService commandService) {
    commandService.startTyping(event.getTextChannel());
    Optional<RarityTypes> requestedRarity = RarityTypes.parse(event.getArgs());
    if (requestedRarity.isEmpty()) {
      event.getMessage().addReaction("U+1F44E").complete();
      String invalidRarity =
          """
          Invalid rarity, acceptable rarities: Common, Uncommon, Rare, Epic, Legendary, Unknown
          """;
      event.getMessage().reply(invalidRarity).queue();
      commandService.stopTyping(event.getTextChannel());
      return;
    }

    event.getMessage().addReaction("U+1F44D").complete();

    Predicate<Message> predicate = x -> !x.getAttachments().isEmpty();
    predicate = predicate.and(x -> !x.getContentRaw().contains("*ignored"));
    var messages =
        commandService.getMessagesFromUserWithFilter(
            event.getTextChannel(), event.getAuthor().getId(), predicate);
    boolean reversed = type == Type.LAST;
    Optional<Message> requestedMessage =
        commandService.getOccurrence(messages, requestedRarity.get(), reversed);
    if (requestedMessage.isPresent()) {
      replyOccurrence(event.getMessage(), requestedMessage.get(), requestedRarity.get(), type);
    } else {
      replyNotFound(event.getMessage(), requestedRarity.get());
    }
    commandService.stopTyping(event.getTextChannel());
  }
}
