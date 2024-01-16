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

import com.google.common.collect.ConcurrentHashMultiset;
import io.quarkus.arc.Unremovable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Singleton;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Handles sending typing request to channels. Synchronized to prevent spamming the channels.
 */
@Unremovable
@Singleton
public class TypingManager {
  private final ConcurrentHashMultiset<TextChannel> typerCountPerChannel = ConcurrentHashMultiset.create();
  private final ConcurrentHashMap<TextChannel, ScheduledFuture<?>> channelTyper = new ConcurrentHashMap<>(0);
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  public TypingManager() {
    executor.setRemoveOnCancelPolicy(true);
  }

  /**
   * <p>Increment number of typers ({@link ScheduledFuture} for {@link TextChannel}.</p>
   * <p>If the channel does not already have a typer, create it.</p>
   * @param channel channel to increment typer count on
   */
  public synchronized void startIfNotRunning(TextChannel channel) {
    if (!typerCountPerChannel.contains(channel)) {
      sendTyping(channel);
    }
    typerCountPerChannel.add(channel);
  }

  /**
   * Create new typer ({@link ScheduledFuture}) and add it to the pool.
   * @param textChannel channel to start the typer on
   */
  private synchronized void sendTyping(TextChannel textChannel) {
    Runnable run = () -> textChannel.sendTyping().complete();
    ScheduledFuture<?> typer = executor.scheduleAtFixedRate(run, 0, 5, TimeUnit.SECONDS);
    channelTyper.put(textChannel, typer);
  }

  /**
   * Decrement number of typers ({@link ScheduledFuture}) for {@link TextChannel}, if
   * the numbers is 0, stop the typer.
   * @param textChannel channel to decrement typer count from
   */
  public synchronized void cancelThread(TextChannel textChannel) {
    typerCountPerChannel.remove(textChannel);
    if (!typerCountPerChannel.contains(textChannel)) {
      ScheduledFuture<?> typer = channelTyper.get(textChannel);
      typer.cancel(true);
      channelTyper.remove(textChannel);
    }
  }
}
