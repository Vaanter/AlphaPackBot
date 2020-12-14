package com.vb.alphapackbot;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.flogger.FluentLogger;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Executes specified process.
 */
public class Processor implements Runnable {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final Properties properties = Properties.getInstance();

  private final List<Message> messages;
  private final GuildMessageReceivedEvent event;
  private final ProcessingCommand command;
  private final RarityTypes requestedRarity;

  private volatile boolean isProcessing = true;

  public Processor(final List<Message> messages,
                   final GuildMessageReceivedEvent event,
                   final ProcessingCommand command,
                   final RarityTypes requestedRarity) {
    this.requestedRarity = requestedRarity;

    this.messages = messages.stream()
        .filter(x -> !x.getAttachments().isEmpty())
        .filter(x -> x.getAuthor().getId().equals(event.getAuthor().getId()))
        .collect(Collectors.toList());
    this.event = event;
    this.command = command;
  }

  public static ProcessorBuilder builder(List<Message> messages,
                                         GuildMessageReceivedEvent event,
                                         ProcessingCommand command) {
    return new ProcessorBuilder(messages, event, command);
  }

  @Override
  public void run() {
    sendTyping(event.getChannel());
    if (command == ProcessingCommand.COUNT) {
      String authorId = event.getAuthor().getId();
      String channelId = event.getChannel().getId();
      UserData userData = getRaritiesForUser(messages, authorId, channelId);
      if (properties.isDatabaseEnabled()) {
        saveToDatabase(userData);
      }
      printRarityPerUser(userData, event.getChannel());
    } else {
      Optional<Message> result;
      if (command == ProcessingCommand.FIRST) {
        result = getOccurrence(Lists.reverse(messages));
      } else {
        result = getOccurrence(messages);
      }
      result.ifPresent(this::printOccurrence);
    }
    isProcessing = false;
    synchronized (properties.getProcessingCounter()) {
      properties.getProcessingCounter().decrement();
    }
  }


  /**
   * Sends typing action while this runnable is running
   *
   * @param textChannel channel to which action is sent
   */
  private void sendTyping(TextChannel textChannel) {
    final Thread typingThread = new Thread(() -> {
      do {
        textChannel.sendTyping().complete();
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          log.atWarning().withCause(e).log("Typing thread interrupted!");
        }
      } while (isProcessing);
    }, "Typing thread");
    typingThread.start();
  }

  /**
   * Finds first occurrence of rarity
   *
   * @param messages list of messages in which the rarity is searched
   * @return {@link Optional} of message (empty if specified rarity is not present)
   */
  private Optional<Message> getOccurrence(List<Message> messages) {
    for (Message message : messages) {
      try {
        String messageUrl = message.getAttachments().get(0).getUrl();
        BufferedImage image = getImage(messageUrl);
        RarityTypes rarity = getRarity(image);
        if (rarity == RarityTypes.UNKNOWN) {
          log.atInfo().log("Unknown rarity in %s!", messageUrl);
        }
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
   * Obtains all rarity data for specific user.
   * <p></p>
   * Check {@link Processor#getRarity(BufferedImage)}, {@link Processor#loadUserData(String, String)}
   *
   * @param messages  Messages from which rarities will be extracted
   * @param authorId  ID of request message author
   * @param channelId ID of channel from which request was sent
   */
  private UserData getRaritiesForUser(@NotNull List<Message> messages,
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

      double processed = 0;
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
        processed += 1;
        double percentage = (processed / messages.size()) * 100;
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

  /**
   * Prints occurrence to console and sends message to channel if enabled.
   *
   * @param message message of the occurrence
   */
  private void printOccurrence(@NotNull Message message) {
    OffsetDateTime timeCreated = message.getTimeCreated();
    String reply = "You opened your " + command.toString() + " "
        + requestedRarity.toString() + " on "
        + timeCreated.getDayOfMonth() + "." + timeCreated.getMonth().getValue() + "." + timeCreated.getYear()
        + " at " + timeCreated.getHour() + ":" + timeCreated.getMinute() + "\n"
        + "link: " + message.getJumpUrl() + ".";

    System.out.println(reply);

    if (properties.isPrintingEnabled()) {
      event.getMessage().reply(reply).complete();
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

  private BufferedImage getImage(@NotNull String imageUrl) throws IOException {
    try (InputStream in = new URL(imageUrl).openStream()) {
      return ImageIO.read(in);
    }
  }

  /**
   * Obtains RarityType value from image.
   *
   * @param image image to be processed
   * @return Rarity from {@link RarityTypes}
   */
  @NotNull
  private RarityTypes getRarity(@NotNull BufferedImage image) {
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
    log.atInfo().log("R: %d G: %d B: %d", colors[0], colors[1], colors[2]);
    return RarityTypes.UNKNOWN;
  }

  public static class ProcessorBuilder {
    private final List<Message> messages;
    private final GuildMessageReceivedEvent event;
    private final ProcessingCommand command;
    private RarityTypes requestedRarity = null;

    public ProcessorBuilder(List<Message> messages,
                            GuildMessageReceivedEvent event,
                            ProcessingCommand command) {
      this.messages = messages;
      this.event = event;
      this.command = command;
    }

    public ProcessorBuilder requestedRarity(RarityTypes requestedRarity) {
      this.requestedRarity = requestedRarity;
      return this;
    }

    public Processor build() {
      return new Processor(messages, event, command, requestedRarity);
    }
  }
}
