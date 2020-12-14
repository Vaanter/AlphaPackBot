package com.vb.alphapackbot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Contains available rarity types and special unknown type.
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
            Range.closed(60, 100),
            Range.closed(60, 100),
            Range.closed(60, 100));
        break;
      case "Uncommon":
        range = ImmutableList.of(
            Range.closed(200, 250),
            Range.closed(185, 235),
            Range.closed(160, 210));
        break;
      case "Rare":
        range = ImmutableList.of(
            Range.closed(55, 105),
            Range.closed(135, 200),
            Range.closed(185, 255));
        break;
      case "Epic":
        range = ImmutableList.of(
            Range.closed(135, 185),
            Range.closed(50, 90),
            Range.closed(155, 210));
        break;
      case "Legendary":
        range = ImmutableList.of(
            Range.closed(235, 255),
            Range.closed(145, 170),
            Range.closed(5, 30));
        break;
      default:
        range = ImmutableList.of(
            Range.closed(0, 0),
            Range.closed(0, 0),
            Range.closed(0, 0));
        break;
    }
  }

  private static final Map<String, RarityTypes> stringValues = Stream.of(values())
      .collect(Collectors.toMap(RarityTypes::toString, x -> x));

  public static Optional<RarityTypes> parse(String toParse) {
    return Optional.ofNullable(stringValues.getOrDefault(toParse, null));
  }

  @Override
  public String toString() {
    return rarity;
  }
}
