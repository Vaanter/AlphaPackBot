package com.vb.alphapackbot;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.mu.util.concurrent.Retryer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import lombok.extern.flogger.Flogger;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@Flogger
@Singleton
public class MessageHandler extends ListenerAdapter {
  private static final String CHANNEL = "pack";
  private static final int MAX_RETRIEVE_SIZE = 100;
  private final Properties properties = Properties.getInstance();
  private final ExecutorService executor = Executors.newFixedThreadPool(10);
  private final ExecutorService typingExecutor = Executors.newCachedThreadPool();
  private Firestore db;

  MessageHandler() {
    try (InputStream serviceAccount = new FileInputStream("serviceAccount.json")) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(credentials)
          .build();
      FirebaseApp.initializeApp(options);

      db = FirestoreClient.getFirestore();
      log.atInfo().log("Database connection established successfully.");
    } catch (IOException e) {
      properties.setDatabaseEnabled(false);
      log.atSevere()
          .log("Unable to establish connection to database! Disabling database functions.");
    }
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (!event.getChannel().getName().contains(CHANNEL) || event.getAuthor().isBot()) {
      return;
    }

    if (event.getMessage().getContentRaw().toLowerCase(Locale.getDefault()).startsWith("*pack")
        && properties.isBotEnabled()) {
      synchronized (properties.getProcessingCounter()) {
        if (properties.getProcessingCounter().longValue() == 0) {
          sendTyping(event.getTextChannel());
        }
      }
      HashSet<User> mentions = new HashSet<>(event.getMessage().getMentionedUsers());
      if (mentions.isEmpty()) {
        mentions.add(event.getAuthor());
      }
      ArrayList<Message> messages = getMessages(event.getTextChannel());
      for (User user : mentions) {
        Runnable runnable = () -> {
          synchronized (properties.getProcessingCounter()) {
            properties.getProcessingCounter().increment();
          }
          System.out.println("Calculating...");
          String authorId = user.getId();
          String channelId = event.getChannel().getId();
          UserData userData = getRaritiesForUser(messages, authorId, channelId);
          if (properties.isDatabaseEnabled()) {
            saveToDatabase(userData);
          }
          printRarityPerUser(userData, event.getTextChannel());
          synchronized (properties.getProcessingCounter()) {
            properties.getProcessingCounter().decrement();
          }
        };
        executor.execute(runnable);
      }
    }
  }

  private void sendTyping(TextChannel textChannel) {
    typingExecutor.execute(() -> {
      do {
        textChannel.sendTyping().complete();
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          log.atWarning().withCause(e).log("Typing thread interrupted!");
        }
      } while (properties.getProcessingCounter().longValue() > 0);
    });
  }

  /**
   * Returns specified amount of messages from specific channel.
   *
   * @param channel channel to get messages from
   * @return ArrayList of messages
   */
  private @NotNull ArrayList<Message> getMessages(@NotNull TextChannel channel) {
    System.out.println("Getting messages...");
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
        log.atWarning()
            .withCause(rateLimitedException)
            .log("Too many requests, waiting 5 seconds.");
      }
      amount -= numToRetrieve;
    }
    return messages;
  }

  /**
   * Obtains all rarity data for specific user.
   * <p></p>
   * Check {@link Bot#getRarity(String)}, {@link Bot#printRarityPerUser(UserData, TextChannel)}
   *
   * @param messages  Messages from which rarities will be extracted
   * @param authorId  ID of request message author
   * @param channelId ID of channel from which request was sent
   */
  private UserData getRaritiesForUser(@NotNull ArrayList<Message> messages,
                                      @NotNull String authorId,
                                      @NotNull String channelId) {
    System.out.println("Getting rarity per user...");
    List<Message> messagesFiltered = messages
        .stream()
        .filter(x -> !x.getAttachments().isEmpty())
        .filter(x -> x.getAuthor().getId().equals(authorId))
        .collect(Collectors.toCollection(ArrayList::new));

    UserData userData;
    if (properties.isDatabaseEnabled()) {
      userData = loadFromDatabase(authorId + "_" + channelId)
          .orElse(new UserData(authorId, channelId));
    } else {
      userData = new UserData(authorId, channelId);
    }
    int processedMessageCount = userData
        .getRarityData()
        .values()
        .stream()
        .reduce(Integer::sum)
        .orElse(0);
    if (processedMessageCount < messagesFiltered.size()) {
      messagesFiltered = messagesFiltered.subList(processedMessageCount, messagesFiltered.size());

      double processed = 0;
      for (Message message : messagesFiltered) {
        RarityTypes rarity = getRarity(message.getAttachments().get(0).getUrl());
        userData.increment(rarity);
        processed += 1;
        double percentage = (processed / messagesFiltered.size()) * 100;
        System.out.println("Processed: " + String.format("%.2f", percentage) + "%");
      }
    }
    return userData;
  }

  /**
   * Saves user data to firestore database.
   *
   * @param userData User data to be saved
   */
  private void saveToDatabase(@NotNull UserData userData) {
    String docId = userData.getAuthorId() + "_" + userData.getChannelId();
    final DocumentReference docRef = db.collection("user_data").document(docId);
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
  private void printRarityPerUser(@NotNull UserData userData, @NotNull TextChannel channel) {
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

    synchronized (properties.getProcessingCounter()) {
      if (properties.getProcessingCounter().longValue() > 0) {
        sendTyping(channel);
      }
    }
  }

  private Optional<UserData> loadFromDatabase(String docId) {
    DocumentReference docRef = db.collection("user_data").document(docId);
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
        return Optional.of(new UserData(
            rarity,
            Preconditions.checkNotNull(document.getString("authorId")),
            Preconditions.checkNotNull(document.getString("channelId"))));
      }
    } catch (InterruptedException | ExecutionException e) {
      log.atSevere()
          .withCause(e)
          .log("Exception occurred while getting messages from database!");
    }
    return Optional.empty();
  }

  /**
   * Obtains RarityType value from image.
   *
   * @param imageUrl Url of image to be processed
   * @return Rarity from {@link RarityTypes}
   */
  @NotNull
  private RarityTypes getRarity(@NotNull String imageUrl) {
    try (InputStream in = new URL(imageUrl).openStream()) {
      BufferedImage image = ImageIO.read(in);
      int width = (int) (image.getWidth() * 0.489583); //~940 @ FHD
      int height = (int) (image.getHeight() * 0.83333); //~900 @ FHD
      Color color = new Color(image.getRGB(width, height));
      int[] colors = {color.getRed(), color.getGreen(), color.getBlue()};

      for (RarityTypes rarity : RarityTypes.values()) {
        ImmutableList<Range<Integer>> range = rarity.getRange();
        int hitCounter = 0;
        for (int i = 0; i < 3; i++) {
          if (range.get(i).contains(colors[i])) {
            hitCounter += 1;
          }
        }
        if (hitCounter == 3) {
          return rarity;
        }
      }
      log.atInfo().log("Unknown rarity in %s!", imageUrl);
      log.atInfo().log("R: %d G: %d B: %d", colors[0], colors[1], colors[2]);
      return RarityTypes.UNKNOWN;
    } catch (IOException e) {
      log.atSevere()
          .withCause(e)
          .log("Exception was thrown while getting image!");
    }
    return RarityTypes.UNKNOWN;
  }
}
