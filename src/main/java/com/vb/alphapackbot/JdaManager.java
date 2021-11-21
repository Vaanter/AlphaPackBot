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

import io.grpc.Status;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.jboss.logging.Logger;

public class JdaManager implements QuarkusApplication {
  private static final Logger log = Logger.getLogger(JdaManager.class);

  JDA mainJda;

  final MessageHandler messageHandler;

  @Inject
  JdaManager(final MessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  /**
   * Attempts to initialize and build a JDA with provided token.
   *
   * @param token Bot token required for authorization.
   * @return An instance of {@link JDA} representing the bot.
   * @throws LoginException if provided token is invalid.
   * @throws InterruptedException if the thread gets interrupted while constructing the JDA
   */
  public JDA initialize(final String token) throws LoginException, InterruptedException {
    final JDABuilder jda = JDABuilder
        .createLight(token)
        .addEventListeners(messageHandler);
    try {
      final JDA mainJda = jda.build();
      mainJda.awaitReady();
      return mainJda;
    } catch (LoginException e) {
      throw new LoginException("Invalid token!");
    } catch (InterruptedException e) {
      throw new InterruptedException("Interrupted while getting ready!");
    }
  }

  @Override
  public int run(final String... args) throws Exception {
    String token = System.getenv("TOKEN");
    if (token == null) {
      log.fatal("You must supply a bot token in environment variable 'TOKEN'.");
      System.exit(1);
    }
    try {
      mainJda = initialize(token);
    } catch (LoginException | InterruptedException e) {
      log.fatal(e);
      System.exit(1);
    }
    Quarkus.waitForExit();
    return 0;
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (mainJda != null) {
      mainJda.shutdownNow();
    }
  }

  /**
   * Listens for 'set-activity' event to change the activity status of the bot.

   * @param request contains the new activity type and activity text if applicable.
   * @return {@link Uni} containing the result as a {@link Status}
   */
  @ConsumeEvent(value = "set-activity")
  public Uni<Status> setActivity(BotStatusRequest request) {
    switch (request.getType()) {
      case PLAYING:
        mainJda.getPresence().setActivity(Activity.playing(request.getName()));
        break;
      case COMPETING:
        mainJda.getPresence().setActivity(Activity.competing(request.getName()));
        break;
      case LISTENING:
        mainJda.getPresence().setActivity(Activity.listening(request.getName()));
        break;
      case WATCHING:
        mainJda.getPresence().setActivity(Activity.watching(request.getName()));
        break;
      case CLEAR:
        mainJda.getPresence().setActivity(null);
        break;
      default:
        return Uni.createFrom().item(() -> Status.INVALID_ARGUMENT);
    }
    return Uni.createFrom().item(() -> Status.OK);
  }
}
