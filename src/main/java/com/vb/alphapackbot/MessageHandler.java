package com.vb.alphapackbot;

import com.google.common.base.Splitter;
import com.google.common.flogger.FluentLogger;
import com.google.mu.util.concurrent.Retryer;
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

/**
 * Handles received messages and starts processor.
 */
public class MessageHandler extends ListenerAdapter {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final int MAX_RETRIEVE_SIZE = 100;
  private static final String invalidCommandMessage = "\nInvalid command, available commands: \n"
      + "count - Counts all rarities\n"
      + "last <rarity> - Prints last occurrence of rarity\n"
      + "first <rarity> - Prints first occurrence of rarity";
  private static final String invalidRarity = "\n Invalid rarity, acceptable rarities: "
      + "Common, Uncommon, Rare, Epic, Legendary, Unknown";

  private static final Properties properties = Properties.getInstance();
  private final ExecutorService executor = Executors.newFixedThreadPool(10);

  public MessageHandler() {
  }

  @Override
  public void onGuildMessageReceived(@Nonnull final GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    if (event.getMessage().getContentStripped().toLowerCase(Locale.getDefault()).startsWith("*pack")
        && properties.isBotEnabled()) {
      Optional<ProcessingCommand> command = parseCommand(event.getMessage().getContentStripped());
      if (command.isEmpty()) {
        if (properties.isPrintingEnabled()) {
          event.getMessage()
              .reply(invalidCommandMessage)
              .complete();
        }
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
          Processor processor = Processor.builder(messages, event, command.get()).build();
          synchronized (properties.getProcessingCounter()) {
            properties.getProcessingCounter().increment();
          }
          executor.execute(processor);
        }
      } else {
        Optional<RarityTypes> rarity = parseRarity(event.getMessage().getContentStripped());
        if (rarity.isEmpty()) {
          if (properties.isPrintingEnabled()) {
            event.getMessage()
                .reply(invalidRarity)
                .complete();
          }
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

  /**
   * Parses command from second position (indexed from 1) in message.
   * <p>Available commands are specified in {@link ProcessingCommand}</p>
   *
   * @param message String representation of message.
   * @return {@link Optional} of {@link ProcessingCommand} or empty if invalid / none command is passed.
   */
  private Optional<ProcessingCommand> parseCommand(@NotNull String message) {
    List<String> messageParts = Splitter.on(" ").splitToList(message);
    if (messageParts.size() > 1) {
      return ProcessingCommand.parse(messageParts.get(1).toLowerCase());
    }
    return Optional.empty();
  }

  /**
   * Parses rarity from third position (indexed from 1) in message.
   * <p>Available rarities are specified in {@link RarityTypes}</p>
   *
   * @param message String representation of message.
   * @return {@link Optional} of {@link RarityTypes} or empty if invalid / none rarity is passed.
   */
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
