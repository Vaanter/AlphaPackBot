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

import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;
import javax.inject.Singleton;

@Singleton
public class Telemetry {
  private final Stopwatch stopwatch = Stopwatch.createStarted();
  private final LongAdder commandsReceived = new LongAdder();


  /**
   * Formats the uptime.
   *
   * @return {@link String} containing the formatted time.
   */
  public String formatUptime() {
    StringBuilder builder = new StringBuilder(11);
    Duration elapsed = stopwatch.elapsed();
    if (elapsed.toDays() > 0) {
      builder.append(String.format("%02d Days ", elapsed.toDays()));
      elapsed = elapsed.minusDays(elapsed.toDays());
    }
    builder.append(String.format("%02dH:", elapsed.toHours()));
    elapsed = elapsed.minusHours(elapsed.toHours());
    builder.append(String.format("%02dM:", elapsed.toMinutes()));
    elapsed = elapsed.minusMinutes(elapsed.toMinutes());
    builder.append(String.format("%02dS", elapsed.toSeconds()));
    return builder.toString();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(40);
    builder.append("Uptime: ").append(formatUptime()).append("\n");
    builder.append("Commands received: ").append(commandsReceived);
    return builder.toString();
  }

  public LongAdder getCommandsReceived() {
    return this.commandsReceived;
  }
}
