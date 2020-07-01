package com.vb.alphapackbot;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

class UserData {
  private final HashMap<RarityTypes, Integer> rarityData;
  private final String authorId;
  private final String channelId;

  UserData(@NotNull HashMap<RarityTypes, Integer> rarityData,
           @NotNull String authorId,
           @NotNull String channelId) {
    this.rarityData = rarityData;
    this.authorId = authorId;
    this.channelId = channelId;
  }

  HashMap<RarityTypes, Integer> getRarityData() {
    return rarityData;
  }

  String getAuthorId() {
    return authorId;
  }

  void replace(RarityTypes rarity) {
    rarityData.replace(rarity, rarityData.get(rarity) + 1);
  }

  String getChannelId() {
    return channelId;
  }
}
