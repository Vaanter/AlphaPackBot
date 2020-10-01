package com.vb.alphapackbot;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Properties {
  private final static Properties instance = new Properties();

  private AtomicBoolean isProcessing = new AtomicBoolean(false);
  private AtomicBoolean isDatabaseEnabled = new AtomicBoolean(true);

  @Setter
  private boolean isPrintingEnabled = true;
  @Setter
  private boolean isCachingEnabled = true;
  @Setter
  private volatile boolean isBotEnabled = true;

  private Properties() {
  }

  public static Properties getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Is bot enabled: " + isBotEnabled);
    builder.append("Is processing: " + isProcessing);
    builder.append("Is database enabled: " + isDatabaseEnabled);
    builder.append("Is printing enabled: " + isPrintingEnabled);
    builder.append("Is caching enabled: " + isCachingEnabled);
    return builder.toString();
  }
}
