package com.vb.alphapackbot;

import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Properties {
  private final static Properties instance = new Properties();

  private final LongAdder processingCounter = new LongAdder();

  /**
   * Flag that enables/disables use of database.
   */
  private volatile boolean databaseEnabled = true;

  /**
   * Flag that enables/disables sending messages to Discord.
   */
  private volatile boolean isPrintingEnabled = true;

  /**
   * Flag that enables/disables all non-management bot processes.
   */
  private volatile boolean isBotEnabled = true;

  private Properties() {
  }

  static Properties getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    return "Is bot enabled: " + isBotEnabled +
        "\nRequests being processed: " + processingCounter.longValue() +
        "\nIs database enabled: " + databaseEnabled +
        "\nIs printing enabled: " + isPrintingEnabled;
  }
}
