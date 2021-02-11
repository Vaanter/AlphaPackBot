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

import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import lombok.Setter;

/**
 * Static properties class.
 */
@Getter
@Setter
public class Properties {
  private static final Properties instance = new Properties();
  private final LongAdder processingCounter = new LongAdder();

  /**
   * Enables/disables cache.
   */
  private boolean cacheEnabled = true;

  /**
   * Enables/disables sending messages to Discord.
   */
  private volatile boolean isPrintingEnabled = true;

  /**
   * Enables/disables all non-management bot processes.
   */
  private volatile boolean isBotEnabled = true;

  public static Properties getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    return "Is bot enabled: " + isBotEnabled
        + "\nRequests being processed: " + processingCounter.longValue()
        + "\nIs cache enabled: " + cacheEnabled
        + "\nIs printing enabled: " + isPrintingEnabled;
  }
}
