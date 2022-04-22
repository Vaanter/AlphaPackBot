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

import com.google.mu.util.concurrent.Retryer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class CommandService {
  private static final Logger log = Logger.getLogger(CommandService.class);
  private static final int MAX_RETRIEVE_SIZE = 100;
  private final ExecutorService commandExecutor = Executors.newFixedThreadPool(30);
  @Inject Cache cache;
  @Inject TypingManager typingManager;

  /**
   * Retrieves filtered messages from a channel sent by a specific user.
   *
   * @param channel channel to fetch messages from
   * @param authorId user ID whose messagese to fetch
   * @param filter filter applied on all messages
   * @return {@link List} of messages
   */
  public @NotNull List<Message> getMessagesFromUserWithFilter(
      @NotNull TextChannel channel,
      @NotNull String authorId,
      @NotNull Predicate<? super Message> filter) {
    return getMessages(channel).stream()
        .filter(x -> x.getAuthor().getId().equals(authorId))
        .filter(filter)
        .collect(Collectors.toList());
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
        List<Message> retrieved =
            new Retryer()
                .upon(
                    RateLimitedException.class,
                    Retryer.Delay.ofMillis(5000).exponentialBackoff(1, 5))
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

  /** Start sending typing requests. */
  public void startTyping(TextChannel channel) {
    typingManager.startIfNotRunning(channel);
  }

  /** Stop sending typing requests. */
  public void stopTyping(TextChannel channel) {
    typingManager.cancelThread(channel);
  }

  /**
   * Obtains all rarity data for specific user. Check {@link
   * RarityTypes#computeRarity(BufferedImage)}
   *
   * @param messages Messages from which rarities will be extracted
   * @param authorId ID of request message author
   * @return returns {@link UserData} containing count of all rarities from user.
   */
  public UserData getRaritiesForUser(@NotNull List<Message> messages, @NotNull String authorId) {
    UserData userData = new UserData(authorId);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (Message message : messages) {
      futures.add(
          CompletableFuture.runAsync(
              () -> {
                RarityTypes rarity = loadOrComputeRarity(message);
                userData.increment(rarity);
              }));
    }
    futures.forEach(CompletableFuture::join);
    return userData;
  }

  /**
   * Attempts to load rarity from cache, if unsuccessful, computes the rarity from Image from URL.
   *
   * @param message message containing the URL of image.
   * @return rarity extracted from image or loaded from cache.
   */
  private RarityTypes loadOrComputeRarity(Message message) {
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
        try {
          rarity = RarityTypes.computeRarity(loadImageFromUrl(messageUrl));
          if (rarity != RarityTypes.UNKNOWN) {
            cache.save(messageUrl, rarity.toString());
          }
        } catch (IOException e) {
          log.error("Exception getting an image!", e);
        }
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
  private BufferedImage loadImageFromUrl(@NotNull String imageUrl) throws IOException {
    try (InputStream in = new URL(imageUrl).openStream()) {
      return ImageIO.read(in);
    }
  }

  /**
   * Finds the occurrence of rarity.
   *
   * @param messages list of messages in which the rarity is searched
   * @param requestedRarity rarity to find
   * @param reverse if true iterates over messages in reverse order
   * @return {@link Optional} of message (empty if specified rarity is not present)
   */
  public Optional<Message> getOccurrence(
      List<Message> messages, RarityTypes requestedRarity, boolean reverse) {
    return reverse
        ? getOccurenceLast(messages, requestedRarity)
        : getOccurenceFirst(messages, requestedRarity);
  }

  private Optional<Message> getOccurenceLast(List<Message> messages, RarityTypes requestedRarity) {
    for (Message message : messages) {
      RarityTypes rarity = loadOrComputeRarity(message);
      if (rarity == requestedRarity) {
        return Optional.of(message);
      }
    }
    return Optional.empty();
  }

  private Optional<Message> getOccurenceFirst(List<Message> messages, RarityTypes requestedRarity) {
    for (int i = messages.size() - 1; i > 0; i--) {
      RarityTypes rarity = loadOrComputeRarity(messages.get(i));
      if (rarity == requestedRarity) {
        return Optional.of(messages.get(i));
      }
    }
    return Optional.empty();
  }
}
