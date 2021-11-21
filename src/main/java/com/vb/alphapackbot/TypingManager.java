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
import javax.inject.Singleton;
import net.dv8tion.jda.api.entities.TextChannel;

@Unremovable
@Singleton
public class TypingManager {
  private final ConcurrentHashMultiset<TextChannel> liveChannels = ConcurrentHashMultiset.create();
  private final ConcurrentHashMap<TextChannel, ScheduledFuture<?>> channelFutures = new ConcurrentHashMap<>(0);
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  public TypingManager() {
    executor.setRemoveOnCancelPolicy(true);
  }

  public synchronized void startIfNotRunning(TextChannel channel) {
    if (!liveChannels.contains(channel)) {
      sendTyping(channel);
    }
    liveChannels.add(channel);
  }

  private void sendTyping(TextChannel textChannel) {
    Runnable run = () -> textChannel.sendTyping().complete();
    ScheduledFuture<?> typer = executor.scheduleAtFixedRate(run, 0, 5, TimeUnit.SECONDS);
    channelFutures.put(textChannel, typer);
  }

  public synchronized void cancelThread(TextChannel textChannel) {
    liveChannels.remove(textChannel);
    if (!liveChannels.contains(textChannel)) {
      ScheduledFuture<?> typer = channelFutures.get(textChannel);
      typer.cancel(true);
      channelFutures.remove(textChannel);
    }
  }
}
