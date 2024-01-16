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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.jboss.logging.Logger;

public class JdaManager {
  private static final Logger log = Logger.getLogger(JdaManager.class);

  @Inject Instance<Command> commands;

  /**
   * Attempts to initialize and build a JDA with token from environment variable 'TOKEN'.
   *
   * @return An instance of {@link JDA} representing the bot API, or null.
   */
  @Produces
  @Singleton
  @Nullable
  public JDA createJda() {
    String token = System.getenv("TOKEN");
    if (token == null) {
      log.fatal("You must supply a bot token in environment variable 'TOKEN'.");
      return null;
    }
    final JDABuilder builder = JDABuilder.createLight(token);
    try {
      var commandClient =
          new CommandClientBuilder()
              .setPrefix("*pack ")
              .setOwnerId(355011687495237632L)
              .addCommands(commands.stream().toArray(Command[]::new))
              .build();
      final JDA jda = builder
          .addEventListeners(commandClient)
          .build();
      jda.awaitReady();
      return jda;
    } catch (LoginException e) {
      log.fatal("Invalid token", e);
    } catch (InterruptedException e) {
      log.fatal("Interrupted while JDA is getting ready!", e);
    }
    return null;
  }
}
