package com.vb.alphapackbot;

import java.util.EnumMap;
import org.jetbrains.annotations.NotNull;

class UserData {
  private final EnumMap<RarityTypes, Integer> rarityData;
  private final String authorId;
  private final String channelId;

  UserData(@NotNull EnumMap<RarityTypes, Integer> rarityData,
           @NotNull String authorId,
           @NotNull String channelId) {
    this.rarityData = rarityData;
    this.authorId = authorId;
    this.channelId = channelId;
  }

  EnumMap<RarityTypes, Integer> getRarityData() {
    return rarityData;
  }

  String getAuthorId() {
    return authorId;
  }

  void increment(RarityTypes rarity) {
    rarityData.replace(rarity, rarityData.get(rarity) + 1);
  }

  String getChannelId() {
    return channelId;
  }
}
