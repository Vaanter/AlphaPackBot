package com.vb.alphapackbot;

import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * POJO holding IDs of author, channel and counts of rarities.
 */
public class UserData {
  private final EnumMap<RarityTypes, Integer> rarityData;
  private final String authorId;
  private final String channelId;

  UserData(@NotNull String authorId,
           @NotNull String channelId) {
    this(null, authorId, channelId);
  }

  UserData(@Nullable EnumMap<RarityTypes, Integer> rarityData,
           @NotNull String authorId,
           @NotNull String channelId) {
    if (rarityData == null) {
      ImmutableMap.Builder<RarityTypes, Integer> builder = ImmutableMap.builderWithExpectedSize(6);
      for (RarityTypes rarityType : RarityTypes.values()) {
        builder.put(rarityType, 0);
      }
      rarityData = new EnumMap<>(builder.build());
    }
    this.authorId = authorId;
    this.channelId = channelId;
    this.rarityData = rarityData;
  }

  EnumMap<RarityTypes, Integer> getRarityData() {
    return rarityData;
  }

  String getAuthorId() {
    return authorId;
  }

  /** Increases count of specified rarity by 1.
   *
   * @param rarity rarity to increment
   */
  void increment(RarityTypes rarity) {
    rarityData.replace(rarity, rarityData.get(rarity) + 1);
  }

  String getChannelId() {
    return channelId;
  }
}
