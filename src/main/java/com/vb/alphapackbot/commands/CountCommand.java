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

import com.google.common.base.Stopwatch;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.vb.alphapackbot.CommandService;
import com.vb.alphapackbot.RarityTypes;
import com.vb.alphapackbot.UserData;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CountCommand extends Command {

  @Inject CommandService commandService;

  public CountCommand() {
    this.name = "count";
    this.help = "Counts amount of skins per rarity";
    this.guildOnly = true;
  }

  @Override
  protected void execute(CommandEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    Stopwatch stopwatch = Stopwatch.createStarted();
    event.getMessage().addReaction("U+1F44D").complete();
    Set<User> mentions = commandService.accumulateUsers(event);
    List<CompletableFuture<Void>> userFutures = new ArrayList<>();
    for (User user : mentions) {
      userFutures.add(
          CompletableFuture.runAsync(
              () -> {
                commandService.startTyping(event.getTextChannel());
                UserData userData = countPerUser(user, event.getTextChannel());
                printRarityPerUser(userData, event.getMessage());
                commandService.stopTyping(event.getTextChannel());
              }));
    }
    userFutures.forEach(CompletableFuture::join);
    Log.info("Time elapsed: " + stopwatch.elapsed());
  }

  private UserData countPerUser(@NotNull User user, @NotNull TextChannel channel) {
    Predicate<Message> predicate = x -> !x.getAttachments().isEmpty();
    predicate = predicate.and(x -> !x.getContentRaw().contains("*ignored"));
    List<Message> messages =
        commandService.getMessagesFromUserWithFilter(channel, user.getId(), predicate);
    return commandService.getRaritiesFromMessages(messages, user.getId());
  }

  /**
   * Sends a message with rarity counts to a channel.
   *
   * @param userData holds the information about user whose packs were counted and also the counts
   *                 themselves
   * @param message  Message which initiated this count, sent message will reply to this message
   */
  public void printRarityPerUser(@NotNull UserData userData, @NotNull Message message) {
    int total =
        userData.getRarityCount(RarityTypes.COMMON)
            + userData.getRarityCount(RarityTypes.UNCOMMON)
            + userData.getRarityCount(RarityTypes.RARE)
            + userData.getRarityCount(RarityTypes.EPIC)
            + userData.getRarityCount(RarityTypes.LEGENDARY)
            + userData.getRarityCount(RarityTypes.UNKNOWN);

    double commonPercentage = (double) userData.getRarityCount(RarityTypes.COMMON) / total;
    double uncommonPercentage = (double) userData.getRarityCount(RarityTypes.UNCOMMON) / total;
    double rarePercentage = (double) userData.getRarityCount(RarityTypes.RARE) / total;
    double epicPercentage = (double) userData.getRarityCount(RarityTypes.EPIC) / total;
    double legendaryPercentage = (double) userData.getRarityCount(RarityTypes.LEGENDARY) / total;
    double unknownPercentage = (double) userData.getRarityCount(RarityTypes.UNKNOWN) / total;

    // @formatter:off
    final String reply = MessageFormat.format("""
        <@{0}>
        Total: {1}
        {2}: {3} ({4, number, percent})
        {5}: {6} ({7, number, percent})
        {8}: {9} ({10, number, percent})
        {11}: {12} ({13, number, percent})
        {14}: {15} ({16, number, percent})
        {17}: {18} ({19, number, percent})
            """,
        userData.getAuthorId(),
        total,
        RarityTypes.COMMON, userData.getRarityCount(RarityTypes.COMMON), commonPercentage,
        RarityTypes.UNCOMMON, userData.getRarityCount(RarityTypes.UNCOMMON), uncommonPercentage,
        RarityTypes.RARE, userData.getRarityCount(RarityTypes.RARE), rarePercentage,
        RarityTypes.EPIC, userData.getRarityCount(RarityTypes.EPIC), epicPercentage,
        RarityTypes.LEGENDARY, userData.getRarityCount(RarityTypes.LEGENDARY), legendaryPercentage,
        RarityTypes.UNKNOWN, userData.getRarityCount(RarityTypes.UNKNOWN), unknownPercentage
    );
    // @formatter:on

    message.reply(reply).queue();
  }
}
