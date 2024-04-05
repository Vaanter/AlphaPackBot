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

import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** POJO holding IDs of author, channel and counts of rarities. */
public class UserData {
  private final Lock lock = new ReentrantLock();
  private final EnumMap<RarityTypes, Integer> rarityData;
  private final String authorId;

  public UserData(@NotNull String authorId) {
    this(null, authorId);
  }

  public UserData(@Nullable EnumMap<RarityTypes, Integer> rarityData, @NotNull String authorId) {
    if (rarityData == null) {
      ImmutableMap.Builder<RarityTypes, Integer> builder = ImmutableMap.builderWithExpectedSize(6);
      for (RarityTypes rarityType : RarityTypes.values()) {
        builder.put(rarityType, 0);
      }
      rarityData = new EnumMap<>(builder.build());
    }
    this.authorId = authorId;
    this.rarityData = rarityData;
  }

  public EnumMap<RarityTypes, Integer> getRarityData() {
    return rarityData;
  }

  public String getAuthorId() {
    return authorId;
  }

  /**
   * Increases count of specified rarity by 1.
   *
   * @param rarity rarity to increment
   */
  public void increment(RarityTypes rarity) {
    lock.lock();
    try {
      rarityData.replace(rarity, rarityData.get(rarity) + 1);
    } finally {
      lock.unlock();
    }
  }

  public int getRarityCount(RarityTypes rarity) {
    return rarityData.getOrDefault(rarity, 0);
  }
}
