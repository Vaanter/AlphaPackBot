package com.vb.alphapackbot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.util.EnumMap;
import lombok.Getter;

public final class RarityData {
  @Getter
  private static ImmutableMap<ImmutableList<Range<Integer>>, RarityTypes> rarities;
  private static ImmutableMap<RarityTypes, Integer> base;

  static {
    Range<Integer> commonRange = Range.closed(70, 100);

    Range<Integer> uncommonRangeRed = Range.closed(200, 215);
    Range<Integer> uncommonRangeGreen = Range.closed(185, 210);
    Range<Integer> uncommonRangeBlue = Range.closed(160, 180);

    Range<Integer> rareRangeRed = Range.closed(55, 80);
    Range<Integer> rareRangeGreen = Range.closed(145, 175);
    Range<Integer> rareRangeBlue = Range.closed(205, 230);

    Range<Integer> epicRangeRed = Range.closed(135, 170);
    Range<Integer> epicRangeGreen = Range.closed(50, 80);
    Range<Integer> epicRangeBlue = Range.closed(155, 195);

    Range<Integer> legendaryRangeRed = Range.closed(245, 255);
    Range<Integer> legendaryRangeGreen = Range.closed(155, 170);
    Range<Integer> legendaryRangeBlue = Range.closed(15, 30);

    var commons = ImmutableList.of(commonRange, commonRange, commonRange, commonRange, commonRange);
    var uncommons = ImmutableList.of(uncommonRangeRed, uncommonRangeGreen, uncommonRangeBlue);
    var rares = ImmutableList.of(rareRangeRed, rareRangeGreen, rareRangeBlue);
    var epics = ImmutableList.of(epicRangeRed, epicRangeGreen, epicRangeBlue);
    var legendaries = ImmutableList.of(legendaryRangeRed, legendaryRangeGreen, legendaryRangeBlue);
    
    
    rarities = ImmutableMap.of(
        commons, RarityTypes.COMMON,
        uncommons, RarityTypes.UNCOMMON,
        rares, RarityTypes.RARE,
        epics, RarityTypes.EPIC,
        legendaries, RarityTypes.LEGENDARY);

    ImmutableMap.Builder<RarityTypes, Integer> builder = ImmutableMap.builderWithExpectedSize(6);
    for (RarityTypes rarityType: RarityTypes.values()) {
      builder.put(rarityType, 0);
    }
    base = builder.build();
  }

  private RarityData() {}

  static EnumMap<RarityTypes, Integer> getBase() {
    return new EnumMap<>(base);
  }
}
