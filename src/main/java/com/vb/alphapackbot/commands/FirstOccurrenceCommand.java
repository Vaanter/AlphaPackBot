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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.vb.alphapackbot.CommandService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class FirstOccurrenceCommand extends OccurrenceCommand {

  @Inject CommandService commandService;

  public FirstOccurrenceCommand() {
    this.name = "first";
    this.help = "Find the first time a rarity was opened";
    this.arguments = "<rarity>";
    this.guildOnly = true;
  }

  @Override
  protected void execute(CommandEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    doCommand(Type.FIRST, event, commandService);
  }
}
