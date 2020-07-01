package com.vb.alphapackbot;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.common.base.Preconditions;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

class Bot extends ListenerAdapter {
  private static final String channel = "alpha-pack";
  private static final int MAX_RETRIEVE_SIZE = 100;
  private static volatile String botMessageId = null;
  private final HashMap<ArrayList<Range<Integer>>, RarityTypes> rarities = new HashMap<>(5);
  private final HashMap<RarityTypes, Integer> base = new HashMap<>(6);
  private final ExecutorService executor = Executors.newFixedThreadPool(3);
  AtomicBoolean isProcessing = new AtomicBoolean(false);
  AtomicBoolean isDatabaseEnabled = new AtomicBoolean(true);
  boolean isPrintingEnabled = true;
  boolean isCachingEnabled = true;
  boolean isBotEnabled = true;
  private Firestore db;
  private volatile CountDownLatch latch;
  private Thread latchThread = null;

  Bot() {
    Range<Integer> commonRange = Range.closed(70, 100);

    Range<Integer> uncommonRangeRed = Range.closed(200, 215);
    Range<Integer> uncommonRangeGreen = Range.closed(185, 210);
    Range<Integer> uncommonRangeBlue = Range.closed(160, 180);

    Range<Integer> rareRangeRed = Range.closed(55, 80);
    Range<Integer> rareRangeGreen = Range.closed(145, 175);
    Range<Integer> rareRangeBlue = Range.closed(205, 230);

    Range<Integer> epicRangeRed = Range.closed(135, 170);
    Range<Integer> epicRangeGreen = Range.closed(50, 80);
    Range<Integer> epicRangeBlue = Range.closed(155, 195);

    Range<Integer> legendaryRangeRed = Range.closed(245, 255);
    Range<Integer> legendaryRangeGreen = Range.closed(155, 170);
    Range<Integer> legendaryRangeBlue = Range.closed(15, 30);

    rarities.put(new ArrayList<>(Arrays.asList(
        commonRange, commonRange, commonRange)), RarityTypes.COMMON);
    rarities.put(new ArrayList<>(Arrays.asList(
        uncommonRangeRed, uncommonRangeGreen, uncommonRangeBlue)), RarityTypes.UNCOMMON);
    rarities.put(new ArrayList<>(Arrays.asList(
        rareRangeRed, rareRangeGreen, rareRangeBlue)), RarityTypes.RARE);
    rarities.put(new ArrayList<>(Arrays.asList(
        epicRangeRed, epicRangeGreen, epicRangeBlue)), RarityTypes.EPIC);
    rarities.put(new ArrayList<>(Arrays.asList(
        legendaryRangeRed, legendaryRangeGreen, legendaryRangeBlue)), RarityTypes.LEGENDARY);

    base.put(RarityTypes.COMMON, 0);
    base.put(RarityTypes.UNCOMMON, 0);
    base.put(RarityTypes.RARE, 0);
    base.put(RarityTypes.EPIC, 0);
    base.put(RarityTypes.LEGENDARY, 0);
    base.put(RarityTypes.UNKNOWN, 0);

    try (InputStream serviceAccount = new FileInputStream("serviceAccount.json")) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      FirebaseOptions options = new FirebaseOptions.Builder()
          .setCredentials(credentials)
          .build();
      FirebaseApp.initializeApp(options);

      db = FirestoreClient.getFirestore();
    } catch (IOException e) {
      Main.logger
          .at(Level.SEVERE)
          .log("Unable to establish connection to database!");
    }
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (!event.getChannel().getName().contains(channel)) {
      return;
    }
    if (event.getAuthor().isBot()
        && event.getMessage().getContentRaw().contains("Processing...")
        && isProcessing.get()) {
      botMessageId = event.getMessageId();
    } else if (event.getAuthor().isBot()) {
      return;
    }

    if (event.getMessage().getContentRaw().toLowerCase().startsWith("*pack") && isBotEnabled) {
      Set<User> mentions = new HashSet<>(event.getMessage().getMentionedUsers());
      if (mentions.isEmpty()) {
        mentions.add(event.getAuthor());
      }
      long latchSize = mentions.size();
      if (isProcessing.get()) {
        latchSize += latch.getCount();
        latchThread.interrupt();
      }
      latch = new CountDownLatch((int) latchSize);
      if (isPrintingEnabled && !isProcessing.get()) {
        event.getChannel().sendMessage("Processing...").complete();
      }
      ArrayList<Message> messages = getMessages(event.getTextChannel());
      for (User user : mentions) {
        Runnable runnable = () -> {
          isProcessing.set(true);
          System.out.println("Calculating...");
          String authorId = user.getId();
          String channelId = event.getChannel().getId();
          getRaritiesForUser(messages, authorId, channelId, event.getChannel());
          latch.countDown();
        };
        executor.execute(runnable);
      }
      System.out.println("Starting latch thread...");
      latchThread = new Thread(() -> {
        try {
          latch.await();
          System.out.println("Latch unblocked!");
          event.getChannel().deleteMessageById(botMessageId).complete();
          isProcessing.set(false);
        } catch (InterruptedException e) {
          System.out.println("New request received while processing, thread interrupted!");
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
  private @NotNull ArrayList<Message> getMessages(TextChannel channel) {
    System.out.println("Getting messages...");
    ArrayList<Message> messages = new ArrayList<>();
    MessageHistory history = channel.getHistory();
    int amount = Integer.MAX_VALUE;

    while (amount > 0) {
      int numToRetrieve = amount;
      if (amount > MAX_RETRIEVE_SIZE) {
        numToRetrieve = MAX_RETRIEVE_SIZE;
      }

      List<Message> retrieved = null;
      try {
        retrieved = history.retrievePast(numToRetrieve).complete(true);
        if (retrieved.isEmpty()) {
          break;
        }
      } catch (RateLimitedException rateLimitedException) {
        Main.logger.at(Level.INFO)
            .withCause(rateLimitedException)
            .log("Too many requests, waiting 5 seconds.");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException interruptedException) {
          Main.logger.at(Level.SEVERE)
              .withCause(interruptedException)
              .log("Thread interrupted while waiting!");
        }
      }
      messages.addAll(retrieved);
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
   * @param channel   Channel used for sending result
   */
  private void getRaritiesForUser(@NotNull ArrayList<Message> messages,
                                  @NotNull String authorId,
                                  @NotNull String channelId,
                                  @NotNull MessageChannel channel) {
    System.out.println("Getting rarity per user...");
    List<Message> messagesFiltered = messages
        .stream()
        .filter(x -> !x.getAttachments().isEmpty())
        .filter(x -> x.getAuthor().getId().equals(authorId))
        .collect(Collectors.toCollection(ArrayList::new));

    UserData temp;
    if (isDatabaseEnabled.get()) {
      temp = loadFromDatabase(authorId + "_" + channelId)
          .orElse(new UserData(new HashMap<>(base), authorId, channelId));
    } else {
      temp = new UserData(new HashMap<>(base), authorId, channelId);
    }
    int processedMessageCount = temp
        .getRarityData()
        .values()
        .stream()
        .reduce(Integer::sum)
        .orElse(0);
    if (!(processedMessageCount >= messagesFiltered.size())) {
      messagesFiltered = messagesFiltered.subList(processedMessageCount, messagesFiltered.size());

      double processed = 0;
      for (Message message : messagesFiltered) {
        RarityTypes rarity = getRarity(message.getAttachments().get(0).getUrl());
        temp.replace(rarity);
        ++processed;
        double percentage = (processed / messagesFiltered.size()) * 100;
        System.out.println("Processed: " + String.format("%.2f", percentage) + "%");
      }
    }
    if (isCachingEnabled) {
      saveToDatabase(temp);
    }
    printRarityPerUser(temp, channel);
  }

  private Optional<UserData> loadFromDatabase(String docId) {
    DocumentReference docRef = db.collection("user_data").document(docId);
    //asynchronous
    ApiFuture<DocumentSnapshot> future = docRef.get();
    try {
      //blocks
      DocumentSnapshot document = future.get();
      if (document.exists()) {
        HashMap<RarityTypes, Integer> rarity = new HashMap<>();
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
      Main.logger.at(Level.SEVERE)
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

      Set<ArrayList<Range<Integer>>> rarityRanges = rarities.keySet();
      ArrayList<RarityTypes> matchingRarities = new ArrayList<>();
      for (ArrayList<Range<Integer>> rarityRange : rarityRanges) {
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
      //System.out.println(matchingRarities);

      Set<RarityTypes> matchingRaritiesCheck = new HashSet<>(matchingRarities);

      HashMap<RarityTypes, Long> occurrences = new HashMap<>();
      for (RarityTypes rarType : matchingRaritiesCheck) {
        long count = matchingRarities.stream().filter(x -> x == rarType).count();
        occurrences.put(rarType, count);
      }

      Map.Entry<RarityTypes, Long> maxEntry = null;

      for (Map.Entry<RarityTypes, Long> entry : occurrences.entrySet()) {
        if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
          maxEntry = entry;
        }
      }

      //ArrayList<Integer> colors =
      //    new ArrayList<>(Arrays.asList(color.getRed(), color.getGreen(), color.getBlue()));
      //System.out.println(colors);
      //System.out.println(matchingRarities);
      return maxEntry != null ? maxEntry.getKey() : RarityTypes.UNKNOWN;
    } catch (IOException e) {
      Main.logger.at(Level.SEVERE)
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
      Main.logger.at(Level.SEVERE)
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

    if (isPrintingEnabled) {
      channel.sendMessage(message).complete();
    }
  }
}
