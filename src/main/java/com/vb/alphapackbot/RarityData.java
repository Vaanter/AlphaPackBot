package com.vb.alphapackbot;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public final class RarityData {
  private static final HashMap<ArrayList<Range<Integer>>, RarityTypes> rarities = new HashMap<>(5);
  private static final HashMap<RarityTypes, Integer> base = new HashMap<>(6);

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

    rarities.put(new ArrayList<>(Arrays.asList(
        commonRange, commonRange, commonRange)), RarityTypes.COMMON);
    rarities.put(new ArrayList<>(Arrays.asList(
        uncommonRangeRed, uncommonRangeGreen, uncommonRangeBlue)), RarityTypes.UNCOMMON);
    rarities.put(new ArrayList<>(Arrays.asList(
        rareRangeRed, rareRangeGreen, rareRangeBlue)), RarityTypes.RARE);
    rarities.put(new ArrayList<>(Arrays.asList(
        epicRangeRed, epicRangeGreen, epicRangeBlue)), RarityTypes.EPIC);
    rarities.put(new ArrayList<>(Arrays.asList(
        legendaryRangeRed, legendaryRangeGreen, legendaryRangeBlue)), RarityTypes.LEGENDARY);

    base.put(RarityTypes.COMMON, 0);
    base.put(RarityTypes.UNCOMMON, 0);
    base.put(RarityTypes.RARE, 0);
    base.put(RarityTypes.EPIC, 0);
    base.put(RarityTypes.LEGENDARY, 0);
    base.put(RarityTypes.UNKNOWN, 0);
  }

  static HashMap<ArrayList<Range<Integer>>, RarityTypes> getRarities() {
    return rarities;
  }

  static HashMap<RarityTypes, Integer> getBase() {
    return new HashMap<>(base);
  }
}
