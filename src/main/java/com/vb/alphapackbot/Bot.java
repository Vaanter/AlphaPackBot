package com.vb.alphapackbot;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import lombok.extern.flogger.Flogger;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@Flogger
class Bot extends ListenerAdapter {
  private static final String CHANNEL = "pack";
  private static final int MAX_RETRIEVE_SIZE = 100;
  private final ExecutorService executor = Executors.newFixedThreadPool(5);
  private final Properties properties = Properties.getInstance();
  private volatile String botMessageId = null;
  private CountDownLatch latch;
  private Thread latchThread = null;
  private Firestore db;

  Bot() {
    try (InputStream serviceAccount = new FileInputStream("serviceAccount.json")) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(credentials)
          .build();
      FirebaseApp.initializeApp(options);

      db = FirestoreClient.getFirestore();
      log.atInfo().log("Database connection established successfully.");
    } catch (IOException e) {
      properties.getIsDatabaseEnabled().set(false);
      properties.setCachingEnabled(false);
      log.atSevere()
          .log("Unable to establish connection to database! Disabling database functions.");
    }
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (!event.getChannel().getName().contains(CHANNEL)) {
      return;
    }
    if (event.getAuthor().isBot()
        && event.getMessage().getContentRaw().contains("Processing...")
        && properties.getIsProcessing().get()) {
      botMessageId = event.getMessageId();
    } else if (event.getAuthor().isBot()) {
      return;
    }

    if (event.getMessage().getContentRaw().toLowerCase(Locale.getDefault()).startsWith("*pack")
        && properties.isBotEnabled()) {
      HashSet<User> mentions = new HashSet<>(event.getMessage().getMentionedUsers());
      if (mentions.isEmpty()) {
        mentions.add(event.getAuthor());
      }
      long latchSize = mentions.size();
      if (properties.getIsProcessing().get()) {
        latchSize += latch.getCount();
        latchThread.interrupt();
      }
      latch = new CountDownLatch((int) latchSize);
      if (properties.isPrintingEnabled() && !properties.getIsProcessing().get()) {
        event.getChannel().sendMessage("Processing...").complete();
      }
      ArrayList<Message> messages = getMessages(event.getTextChannel());
      for (User user : mentions) {
        Runnable runnable = () -> {
          properties.getIsProcessing().set(true);
          log.atInfo().log("Calculating...");
          String authorId = user.getId();
          String channelId = event.getChannel().getId();
          UserData userData = getRaritiesForUser(messages, authorId, channelId);
          if (properties.isCachingEnabled()) {
            saveToDatabase(userData);
          }
          printRarityPerUser(userData, event.getChannel());
          latch.countDown();
        };
        executor.execute(runnable);
      }
      System.out.println("Starting latch thread...");
      latchThread = new Thread(() -> {
        try {
          latch.await();
          log.atInfo().log("Latch unblocked!");
          event.getChannel().deleteMessageById(botMessageId).complete();
          properties.getIsProcessing().set(false);
        } catch (InterruptedException e) {
          log.atInfo().log("New request received while processing, thread interrupted!");
        }
      }, "latchThread");
      latchThread.start();
    }
  }

  /**
   * Returns specified amount of messages from specific channel.
   *
   * @param channel channel to get messages from
   * @return ArrayList of messages
   * @see <a href="https://www.programcreek.com/java-api-examples/?api=net.dv8tion.jda.core.MessageHistory">Source</a>
   */
  private @NotNull ArrayList<Message> getMessages(@NotNull TextChannel channel) {
    System.out.println("Getting messages...");
    ArrayList<Message> messages = new ArrayList<>();
    MessageHistory history = channel.getHistory();
    int amount = Integer.MAX_VALUE;

    while (amount > 0) {
      int numToRetrieve = amount > MAX_RETRIEVE_SIZE ? MAX_RETRIEVE_SIZE : amount;

      try {
        List<Message> retrieved = history.retrievePast(numToRetrieve).complete(true);
        messages.addAll(retrieved);
        if (retrieved.isEmpty()) {
          break;
        }
      } catch (RateLimitedException rateLimitedException) {
        log.atWarning()
            .withCause(rateLimitedException)
            .log("Too many requests, waiting 5 seconds.");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException interruptedException) {
          log.atSevere()
              .withCause(interruptedException)
              .log("Thread interrupted while waiting!");
        }
      }
      
      amount -= numToRetrieve;
    }
    return messages;
  }

  /**
   * Obtains and prints all rarity data for specific user.
   * <p></p>
   * Check {@link Bot#getRarity(String)}, {@link Bot#printRarityPerUser(UserData, MessageChannel)}
   *
   * @param messages  Messages from which rarities will be extracted
   * @param authorId  ID of request message author
   * @param channelId ID of channel from which request was sent
   * @param CHANNEL   Channel used for sending result
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
    if (properties.getIsDatabaseEnabled().get()) {
      userData = loadFromDatabase(authorId + "_" + channelId)
          .orElse(new UserData(RarityData.getBase(), authorId, channelId));
    } else {
      userData = new UserData(RarityData.getBase(), authorId, channelId);
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
        ++processed;
        double percentage = (processed / messagesFiltered.size()) * 100;
        System.out.println("Processed: " + String.format("%.2f", percentage) + "%");
      }
    }
    return userData;
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
        return Optional.of(new UserData(rarity,
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
      int red = color.getRed();
      int green = color.getGreen();
      int blue = color.getBlue();

      ImmutableMap<ImmutableList<Range<Integer>>, RarityTypes> rarities = RarityData.getRarities();
      ImmutableSet<ImmutableList<Range<Integer>>> rarityRanges = rarities.keySet();
      HashMultiset<RarityTypes> matchingRarities = HashMultiset.create();
      for (ImmutableList<Range<Integer>> rarityRange : rarityRanges) {
        if (rarityRange.get(0).contains(red)) {
          matchingRarities.add(rarities.get(rarityRange));
        }
        if (rarityRange.get(1).contains(green)) {
          matchingRarities.add(rarities.get(rarityRange));
        }
        if (rarityRange.get(2).contains(blue)) {
          matchingRarities.add(rarities.get(rarityRange));
        }
      }

      if (red != blue && red != green) {
        matchingRarities.removeIf(x -> x.equals(RarityTypes.COMMON));
      }

      matchingRarities.removeIf(e -> matchingRarities.count(e) < 3);

      RarityTypes maxEntry = null;

      for (RarityTypes entry : matchingRarities.elementSet()) {
        if (maxEntry == null || matchingRarities.count(entry) > matchingRarities.count(maxEntry)) {
          maxEntry = entry;
        }
      }

      return maxEntry != null ? maxEntry : RarityTypes.UNKNOWN;
    } catch (IOException e) {
      log.atSevere()
          .withCause(e)
          .log("Exception was thrown while getting image!");
    }
    return RarityTypes.UNKNOWN;
  }

  /**
   * Saves user data to database.
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
   * Prints data text to console and sends message to channel if enabled.
   *
   * @param userData Data to be printed
   * @param channel  Channel to print data to
   */
  private void printRarityPerUser(@NotNull UserData userData, @NotNull MessageChannel channel) {
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
}
