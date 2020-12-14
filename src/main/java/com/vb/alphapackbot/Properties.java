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

import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.common.flogger.FluentLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import lombok.Setter;


/**
 * Static properties class.
 */
@Getter
@Setter
public class Properties {
  private final static FluentLogger log = FluentLogger.forEnclosingClass();
  private final static Properties instance = new Properties();
  private Firestore db = null;

  private final LongAdder processingCounter = new LongAdder();

  /**
   * Enables/disables use of database.
   */
  private volatile boolean databaseEnabled = true;

  /**
   * Enables/disables sending messages to Discord.
   */
  private volatile boolean isPrintingEnabled = true;

  /**
   * Enables/disables all non-management bot processes.
   */
  private volatile boolean isBotEnabled = true;

  private Properties() {
    try {
      GoogleCredentials credentials = ComputeEngineCredentials.getApplicationDefault();
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(credentials)
          .setProjectId("alpha-pack-bot")
          .build();
      FirebaseApp.initializeApp(options);
      db = FirestoreClient.getFirestore();
      log.atInfo().log("Database connection established successfully.");
    } catch (
        IOException e) {
      instance.setDatabaseEnabled(false);
      log.atSevere()
          .log("Unable to establish connection to database! Disabling database functions.");
    }
  }

  public static Properties getInstance() {
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
