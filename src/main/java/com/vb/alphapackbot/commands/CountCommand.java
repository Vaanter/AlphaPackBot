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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.dv8tion.jda.api.entities.Member;
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
    HashSet<User> mentions = accumulateUsers(event);
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

  @NotNull
  private HashSet<User> accumulateUsers(@NotNull CommandEvent event) {
    HashSet<User> mentions = new HashSet<>();
    if (!event.getMessage().getMentionedRoles().isEmpty()) {
      event.getGuild().getMembersWithRoles(event.getMessage().getMentionedRoles()).stream()
          .map(Member::getUser)
          .forEach(mentions::add);
    }
    if (!event.getMessage().getMentionedUsers().isEmpty()) {
      mentions.addAll(event.getMessage().getMentionedUsers());
    }
    if (mentions.isEmpty()) {
      mentions.add(event.getAuthor());
    }
    return mentions;
  }

  private UserData countPerUser(@NotNull User user, @NotNull TextChannel channel) {
    Predicate<Message> predicate = x -> !x.getAttachments().isEmpty();
    predicate = predicate.and(x -> !x.getContentRaw().contains("*ignored"));
    List<Message> messages =
        commandService.getMessagesFromUserWithFilter(channel, user.getId(), predicate);
    return commandService.getRaritiesForUser(messages, user.getId());
  }

  /**
   * Sends a message with rarity counts to a channel.
   *
   * @param userData Data to be printed
   * @param message Message to reply to
   */
  public void printRarityPerUser(@NotNull UserData userData, @NotNull Message message) {
    int total =
        userData.getRarityData().get(RarityTypes.COMMON)
            + userData.getRarityData().get(RarityTypes.UNCOMMON)
            + userData.getRarityData().get(RarityTypes.RARE)
            + userData.getRarityData().get(RarityTypes.EPIC)
            + userData.getRarityData().get(RarityTypes.LEGENDARY)
            + userData.getRarityData().get(RarityTypes.UNKNOWN);

    String reply =
        "<@"
            + userData.getAuthorId()
            + ">\n"
            + "Total: "
            + total
            + " \n"
            + RarityTypes.COMMON
            + ": "
            + userData.getRarityData().get(RarityTypes.COMMON)
            + "\n"
            + RarityTypes.UNCOMMON
            + ": "
            + userData.getRarityData().get(RarityTypes.UNCOMMON)
            + "\n"
            + RarityTypes.RARE
            + ": "
            + userData.getRarityData().get(RarityTypes.RARE)
            + "\n"
            + RarityTypes.EPIC
            + ": "
            + userData.getRarityData().get(RarityTypes.EPIC)
            + "\n"
            + RarityTypes.LEGENDARY
            + ": "
            + userData.getRarityData().get(RarityTypes.LEGENDARY)
            + "\n"
            + RarityTypes.UNKNOWN
            + ": "
            + userData.getRarityData().get(RarityTypes.UNKNOWN);

    message.reply(reply).queue();
  }
}
