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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.flogger.FluentLogger;
import com.vb.alphapackbot.ProcessingCommand;
import com.vb.alphapackbot.Properties;
import com.vb.alphapackbot.RarityTypes;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all Commands.
 */
public abstract class AbstractCommand implements Runnable {
  static final Properties properties = Properties.getInstance();
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  final List<Message> messages;
  final GuildMessageReceivedEvent event;
  final ProcessingCommand command;
  volatile boolean isProcessing = true;

  AbstractCommand(final List<Message> messages,
                  final GuildMessageReceivedEvent event,
                  final ProcessingCommand command) {

    this.messages = messages.stream()
        .filter(x -> !x.getAttachments().isEmpty())
        .filter(x -> x.getAuthor().getId().equals(event.getAuthor().getId()))
        .collect(Collectors.toList());
    this.event = event;
    this.command = command;
    sendTyping(event.getChannel());
  }

  /**
   * Sends typing action while command is being processed.
   *
   * @param textChannel channel to which action is sent
   */
  private void sendTyping(TextChannel textChannel) {
    final Thread typingThread = new Thread(() -> {
      do {
        textChannel.sendTyping().complete();
        System.out.println("Typing!");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          log.atWarning().withCause(e).log("Typing thread interrupted!");
        }
      } while (isProcessing);
    }, "Typing thread");
    typingThread.setDaemon(true);
    typingThread.start();
  }

  public synchronized void finish() {
    isProcessing = false;
    properties.getProcessingCounter().decrement();
  }

  /**
   * Loads image from an URL into a BufferedImage.
   *
   * @param imageUrl URL from which to load image
   * @return {@link BufferedImage}
   * @throws IOException if an I/O exception occurs.
   */
  public BufferedImage getImage(@NotNull String imageUrl) throws IOException {
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
  public RarityTypes getRarity(@NotNull BufferedImage image) {
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
}
