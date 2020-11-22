package com.vb.alphapackbot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import lombok.Getter;

/**
 * Types of rarities.
 */
public enum RarityTypes {
  COMMON("Common"),
  UNCOMMON("Uncommon"),
  RARE("Rare"),
  EPIC("Epic"),
  LEGENDARY("Legendary"),
  UNKNOWN("Unknown");

  private final String rarity;
  @Getter
  private final ImmutableList<Range<Integer>> range;

  RarityTypes(String rarity) {
    this.rarity = rarity;
    switch (rarity) {
      case "Common":
        range = ImmutableList.of(
            Range.closed(70, 100),
            Range.closed(70, 100),
            Range.closed(70, 100));
        break;
      case "Uncommon":
        range = ImmutableList.of(
            Range.closed(200, 235),
            Range.closed(185, 220),
            Range.closed(160, 195));
        break;
      case "Rare":
        range = ImmutableList.of(
            Range.closed(55, 95),
            Range.closed(145, 190),
            Range.closed(205, 250));
        break;
      case "Epic":
        range = ImmutableList.of(
            Range.closed(135, 175),
            Range.closed(50, 90),
            Range.closed(155, 200));
        break;
      case "Legendary":
        range = ImmutableList.of(
            Range.closed(245, 255),
            Range.closed(155, 170),
            Range.closed(15, 30));
        break;
      default:
        range = ImmutableList.of(
            Range.closed(0, 0),
            Range.closed(0, 0),
            Range.closed(0, 0));
        break;
    }
  }


  @Override
  public String toString() {
    return rarity;
  }
}
