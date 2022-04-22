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
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class Application implements QuarkusApplication {

  @Inject JDA jda;

  @Override
  public int run(String... args) throws Exception {
    if (jda == null) {
      System.exit(1);
    }
    Quarkus.waitForExit();
    return 0;
  }

  void onStop(@Observes ShutdownEvent ev) {
    Log.info("Shutdown");
    if (jda != null) {
      jda.shutdownNow();
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
        jda.getPresence().setActivity(Activity.playing(request.getName()));
        break;
      case COMPETING:
        jda.getPresence().setActivity(Activity.competing(request.getName()));
        break;
      case LISTENING:
        jda.getPresence().setActivity(Activity.listening(request.getName()));
        break;
      case WATCHING:
        jda.getPresence().setActivity(Activity.watching(request.getName()));
        break;
      case CLEAR:
        jda.getPresence().setActivity(null);
        break;
      default:
        return Uni.createFrom().item(() -> Status.INVALID_ARGUMENT);
    }
    return Uni.createFrom().item(() -> Status.OK);
  }
}
