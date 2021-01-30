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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.common.flogger.FluentLogger;
import com.vb.alphapackbot.Commands;
import com.vb.alphapackbot.RarityTypes;
import com.vb.alphapackbot.UserData;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class CountCommand extends AbstractCommand {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public CountCommand(final List<Message> messages,
                      final GuildMessageReceivedEvent event,
                      final Commands command) {
    super(messages, event, command);
  }

  @Override
  public void run() {
    String authorId = event.getAuthor().getId();
    String channelId = event.getChannel().getId();
    UserData userData = getRaritiesForUser(messages, authorId, channelId);
    if (properties.isDatabaseEnabled()) {
      saveToDatabase(userData);
    }
    printRarityPerUser(userData, event.getChannel());
    finish();
  }

  /**
   * Obtains all rarity data for specific user.
   * Check {@link CountCommand#getRarity(BufferedImage)},
   * {@link CountCommand#loadUserData(String, String)}
   *
   * @param messages  Messages from which rarities will be extracted
   * @param authorId  ID of request message author
   * @param channelId ID of channel from which request was sent
   */
  public UserData getRaritiesForUser(@NotNull List<Message> messages,
                                     @NotNull String authorId,
                                     @NotNull String channelId) {
    System.out.println("Getting rarity per user...");
    UserData userData = loadUserData(authorId, channelId);
    int processedMessageCount = userData
        .getRarityData()
        .values()
        .stream()
        .reduce(Integer::sum)
        .orElse(0);
    if (processedMessageCount < messages.size()) {
      messages = messages.subList(processedMessageCount, messages.size());

      for (Message message : messages) {
        try {
          String messageUrl = message.getAttachments().get(0).getUrl();
          BufferedImage image = getImage(messageUrl);
          RarityTypes rarity = getRarity(image);
          if (rarity == RarityTypes.UNKNOWN) {
            log.atInfo().log("Unknown rarity in %s!", messageUrl);
          }
          userData.increment(rarity);
        } catch (IOException e) {
          log.atSevere().withCause(e).log("Exception getting image!");
        }
      }
    }
    return userData;
  }

  /**
   * Saves user data to firestore database.
   *
   * @param userData User data to be saved
   */
  public void saveToDatabase(@NotNull UserData userData) {
    String docId = userData.getAuthorId() + "_" + userData.getChannelId();
    final DocumentReference docRef = properties.getDb().collection("user_data").document(docId);
    HashMap<String, Object> data = new HashMap<>();
    for (Map.Entry<RarityTypes, Integer> rarity : userData.getRarityData().entrySet()) {
      data.put(rarity.getKey().toString(), rarity.getValue());
    }
    data.put("authorId", userData.getAuthorId());
    data.put("channelId", userData.getChannelId());

    //asynchronous
    ApiFuture<WriteResult> result = docRef.set(data);
    //blocks
    try {
      System.out.println("Write time : " + result.get().getUpdateTime());
    } catch (InterruptedException | ExecutionException e) {
      log.atSevere()
          .withCause(e)
          .log("Exception was thrown while saving data to database!");
    }
  }

  /**
   * Prints user data to console and sends message to channel if enabled.
   *
   * @param userData Data to be printed
   * @param channel  Channel to print data to
   */
  public void printRarityPerUser(@NotNull UserData userData, @NotNull TextChannel channel) {
    int total = userData.getRarityData().get(RarityTypes.COMMON)
        + userData.getRarityData().get(RarityTypes.UNCOMMON)
        + userData.getRarityData().get(RarityTypes.RARE)
        + userData.getRarityData().get(RarityTypes.EPIC)
        + userData.getRarityData().get(RarityTypes.LEGENDARY)
        + userData.getRarityData().get(RarityTypes.UNKNOWN);

    String message = "<@" + userData.getAuthorId() + ">\n"
        + "Total: " + total + " \n"
        + RarityTypes.COMMON + ": " + userData.getRarityData().get(RarityTypes.COMMON) + "\n"
        + RarityTypes.UNCOMMON + ": " + userData.getRarityData().get(RarityTypes.UNCOMMON) + "\n"
        + RarityTypes.RARE + ": " + userData.getRarityData().get(RarityTypes.RARE) + "\n"
        + RarityTypes.EPIC + ": " + userData.getRarityData().get(RarityTypes.EPIC) + "\n"
        + RarityTypes.LEGENDARY + ": " + userData.getRarityData().get(RarityTypes.LEGENDARY) + "\n"
        + RarityTypes.UNKNOWN + ": " + userData.getRarityData().get(RarityTypes.UNKNOWN);

    System.out.println(message);

    if (properties.isPrintingEnabled()) {
      channel.sendMessage(message).complete();
    }
  }

  /**
   * Retrieves user data from database, if database is disabled or user is not in database
   * new {@link UserData} object is created.
   *
   * @param authorId  id of user
   * @param channelId id of channel
   * @return {@link Optional} of user data
   */
  private UserData loadUserData(String authorId, String channelId) {
    if (properties.isDatabaseEnabled()) {
      String docId = authorId + "_" + channelId;
      DocumentReference docRef = properties.getDb().collection("user_data").document(docId);
      //asynchronous
      ApiFuture<DocumentSnapshot> future = docRef.get();
      try {
        //blocks
        DocumentSnapshot document = future.get();
        if (document.exists()) {
          EnumMap<RarityTypes, Integer> rarity = new EnumMap<>(RarityTypes.class);
          for (RarityTypes type : RarityTypes.values()) {
            Long databaseObject = document.getLong(type.toString());
            if (databaseObject != null) {
              rarity.put(type, databaseObject.intValue());
            }
          }
          return new UserData(rarity, authorId, channelId);
        }
      } catch (InterruptedException | ExecutionException e) {
        log.atSevere()
            .withCause(e)
            .log("Exception occurred while getting messages from database!");
      }
    }
    return new UserData(authorId, channelId);
  }
}
