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

import com.vb.alphapackbot.Properties;
import com.vb.alphapackbot.Telemetry;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class StatusCommand {
  private static final Properties properties = Properties.getInstance();
  private static final Telemetry telemetry = Telemetry.getInstance();

  private final GuildMessageReceivedEvent event;

  public StatusCommand(final GuildMessageReceivedEvent event) {
    this.event = event;
  }

  public void sendStatus() {
    if (properties.isPrintingEnabled()) {
      event.getChannel().sendMessage(telemetry.toString()).complete();
    }
  }
}
