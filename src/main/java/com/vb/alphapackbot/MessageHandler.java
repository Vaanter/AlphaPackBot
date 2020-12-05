package com.vb.alphapackbot;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.common.base.Splitter;
import com.google.common.flogger.FluentLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.mu.util.concurrent.Retryer;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class MessageHandler extends ListenerAdapter {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final String CHANNEL = "pack";
  private static final int MAX_RETRIEVE_SIZE = 100;
  private static final String invalidCommandMessage = "\nInvalid command, available commands: \n"
      + "count - Counts all rarities\n"
      + "last <rarity> - Prints last occurrence of rarity\n"
      + "first <rarity> - Prints first occurrence of rarity";
  private static final String invalidRarity = "\n Invalid rarity, acceptable rarities: "
      + "Common, Uncommon, Rare, Epic, Legendary, Unknown";

  private final Properties properties = Properties.getInstance();
  private final ExecutorService executor = Executors.newFixedThreadPool(10);
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
  public void onGuildMessageReceived(@Nonnull final GuildMessageReceivedEvent event) {
    if (!event.getChannel().getName().contains(CHANNEL) || event.getAuthor().isBot()) {
      return;
    }
    if (event.getMessage().getContentStripped().toLowerCase(Locale.getDefault()).startsWith("*pack")
        && properties.isBotEnabled()) {
      Optional<ProcessingCommand> command = parseCommand(event.getMessage().getContentStripped());
      if (command.isEmpty()) {
        event.getMessage()
            .reply(invalidCommandMessage)
            .complete();
        return;
      }
      ArrayList<Message> messages = getMessages(event.getChannel());
      if (command.get() == ProcessingCommand.COUNT) {
        HashSet<User> mentions = new HashSet<>();
        event.getGuild()
            .getMembersWithRoles(event.getMessage().getMentionedRoles())
            .stream()
            .map(Member::getUser)
            .forEach(mentions::add);
        mentions.addAll(event.getMessage().getMentionedUsers());
        if (mentions.isEmpty()) {
          mentions.add(event.getAuthor());
        }
        for (int i = 0; i < mentions.size(); i++) {
          Processor processor = Processor.builder(messages, event, command.get()).db(db).build();
          synchronized (properties.getProcessingCounter()) {
            properties.getProcessingCounter().increment();
          }
          executor.execute(processor);
        }
      } else {
        Optional<RarityTypes> rarity = parseRarity(event.getMessage().getContentStripped());
        if (rarity.isEmpty()) {
          event.getMessage()
              .reply(invalidRarity)
              .complete();
          return;
        }
        Processor processor = Processor.builder(messages, event, command.get())
            .requestedRarity(rarity.get())
            .build();
        synchronized (properties.getProcessingCounter()) {
          properties.getProcessingCounter().increment();
        }
        executor.execute(processor);
      }
    }
  }

  private Optional<ProcessingCommand> parseCommand(@NotNull String message) {
    List<String> messageParts = Splitter.on(" ").splitToList(message);
    if (messageParts.size() > 1) {
      return ProcessingCommand.parse(messageParts.get(1).toLowerCase());
    }
    return Optional.empty();
  }

  private Optional<RarityTypes> parseRarity(@NotNull String message) {
    List<String> messageParts = Splitter.on(" ").splitToList(message);
    if (messageParts.size() > 2) {
      String messageRarity = StringUtils.capitalize(messageParts.get(2).toLowerCase());
      return RarityTypes.parse(messageRarity);
    }
    return Optional.empty();
  }

  /**
   * Returns all messages from specific channel.
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
}
