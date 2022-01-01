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

import com.google.mu.util.concurrent.Retryer;
import com.vb.alphapackbot.Cache;
import com.vb.alphapackbot.Commands;
import com.vb.alphapackbot.Properties;
import com.vb.alphapackbot.RarityTypes;
import com.vb.alphapackbot.TypingManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

/** Base class for all Commands. */
public abstract class AbstractCommand implements Runnable {
  private static final Logger log = Logger.getLogger(AbstractCommand.class);
  private static final int MAX_RETRIEVE_SIZE = 100;
  final String authorId;
  final GuildMessageReceivedEvent event;
  final Commands command;
  final Cache cache;
  final TypingManager typingManager;
  final Properties properties;

  AbstractCommand(
      final String authorId,
      final GuildMessageReceivedEvent event,
      final Commands command,
      final Properties properties,
      final Cache cache,
      final TypingManager typingManager) {
    this.properties = properties;
    this.authorId = authorId;
    this.event = event;
    this.command = command;
    this.cache = cache;
    this.typingManager = typingManager;
    typingManager.startIfNotRunning(event.getChannel());
  }

  /**
   * Returns all messages from specific channel.
   *
   * @param channel channel to get messages from
   * @return ArrayList of messages
   */
  private @NotNull ArrayList<Message> getMessages(@NotNull TextChannel channel) {
    ArrayList<Message> messages = new ArrayList<>();
    MessageHistory history = channel.getHistory();
    int amount = Integer.MAX_VALUE;

    while (amount > 0) {
      int numToRetrieve = Math.min(amount, MAX_RETRIEVE_SIZE);

      try {
        List<Message> retrieved = new Retryer()
            .upon(RateLimitedException.class, Retryer.Delay.ofMillis(5000).exponentialBackoff(1, 5))
            .retryBlockingly(() -> history.retrievePast(numToRetrieve).complete(true));
        messages.addAll(retrieved);
        if (retrieved.isEmpty()) {
          break;
        }
      } catch (RateLimitedException rateLimitedException) {
        log.warn("Too many requests, waiting 5 seconds.");
      }
      amount -= numToRetrieve;
    }
    return messages;
  }

  public @NotNull List<Message> getMessagesFromUserWithFilter(
      @NotNull TextChannel channel,
      @NotNull String authorId,
      @NotNull Predicate<? super Message> filter) {
    return getMessages(channel)
        .stream()
        .filter(x -> x.getAuthor().getId().equals(authorId))
        .filter(filter)
        .collect(Collectors.toList());
  }

  /**
   * Stop sending typing requests and decrement processing couter.
   */
  public void finish() {
    typingManager.cancelThread(event.getChannel());
    properties.getProcessingCounter().decrement();
  }

  /**
   * Attempts to load rarity from cache, if unsuccessful, computes the rarity from Image from URL.
   *
   * @param message message containing the URL of image.
   * @return rarity extracted from image or loaded from cache.
   * @throws IOException if an I/O exception occurs.
   */
  public RarityTypes loadOrComputeRarity(Message message) throws IOException {
    String messageUrl = message.getAttachments().get(0).getUrl();
    RarityTypes rarity = null;
    if (!message.getContentRaw().isEmpty() && message.getContentRaw().startsWith("*")) {
      Optional<RarityTypes> forcedRarity = RarityTypes.parse(message.getContentRaw().substring(1));
      if (forcedRarity.isPresent()) {
        rarity = forcedRarity.get();
      }
    }
    if (rarity == null) {
      Optional<RarityTypes> cachedValue = cache.getAndParse(messageUrl);
      rarity = cachedValue.orElse(RarityTypes.UNKNOWN);
      if (cachedValue.isEmpty()) {
        rarity = RarityTypes.computeRarity(loadImageFromUrl(messageUrl));
        cache.save(messageUrl, rarity.toString());
      }
    }
    if (rarity == RarityTypes.UNKNOWN) {
      log.infof("Unknown rarity in %s!", messageUrl);
    }
    return rarity;
  }

  /**
   * Loads image from an URL into a BufferedImage.
   *
   * @param imageUrl URL from which to load image
   * @return {@link BufferedImage}
   * @throws IOException if an I/O exception occurs.
   */
  public BufferedImage loadImageFromUrl(@NotNull String imageUrl) throws IOException {
    try (InputStream in = new URL(imageUrl).openStream()) {
      return ImageIO.read(in);
    }
  }
}
