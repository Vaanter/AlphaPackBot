package com.vb.alphapackbot;

/** Types of rarities.
 */
public enum RarityTypes {
  COMMON("Common"),
  UNCOMMON("Uncommon"),
  RARE("Rare"),
  EPIC("Epic"),
  LEGENDARY("Legendary"),
  UNKNOWN("Unknown");

  private final String rarity;

  RarityTypes(String rarity) {
    this.rarity = rarity;
  }

  @Override
  public String toString() {
    return rarity;
  }
}
