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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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

  private static final ImmutableBiMap<RarityTypes, String> stringValueMap = Stream.of(values())
      .collect(ImmutableBiMap.toImmutableBiMap(x -> x, RarityTypes::toString));
  private static final int MAX_COLOR_DISTANCE = 3;
  private final String rarity;
  private final ImmutableList<ColorRanges> colorRanges;

  RarityTypes(@NotNull String rarity) {
    this.rarity = rarity;
    switch (rarity) {
      case "Common" -> colorRanges = ImmutableList.of(
          // Old ranges
          new ColorRanges(
              Range.closed(80, 100),
              Range.closed(80, 100),
              Range.closed(80, 100)),
          // New ranges
          new ColorRanges(
              Range.closed(160, 180),
              Range.closed(160, 180),
              Range.closed(160, 180)),
          // New ranges duplicate
          new ColorRanges(
              Range.closed(60, 80),
              Range.closed(60, 80),
              Range.closed(60, 80))
      );
      case "Uncommon" -> colorRanges = ImmutableList.of(
          // Old ranges
          new ColorRanges(
              Range.closed(200, 250),
              Range.closed(185, 235),
              Range.closed(160, 210)),
          // New ranges duplicate
          new ColorRanges(
              Range.closed(40, 55),
              Range.closed(75, 90),
              Range.closed(50, 60)),
          // Newer ranges duplicate
          new ColorRanges(
              Range.closed(60, 70),
              Range.closed(140, 150),
              Range.closed(90, 110)
          ),
          // New ranges
          new ColorRanges(
              Range.closed(70, 95),
              Range.closed(190, 220),
              Range.closed(120, 150)),
          // New ranges duplicates
          new ColorRanges(
              Range.closed(60, 70),
              Range.closed(140, 150),
              Range.closed(90, 110))
      );
      case "Rare" -> colorRanges = ImmutableList.of(
          // New ranges
          new ColorRanges(
              Range.closed(55, 105),
              Range.closed(135, 200),
              Range.closed(185, 255)),
          // New ranges duplicate
          new ColorRanges(
              Range.closed(30, 40),
              Range.closed(60, 70),
              Range.closed(75, 90)),
          // Duplicates
          new ColorRanges(
              Range.closed(45, 55),
              Range.closed(105, 125),
              Range.closed(140, 165)
          )
      );
      case "Epic" -> colorRanges = ImmutableList.of(
          // New ranges
          new ColorRanges(
              Range.closed(125, 185),
              Range.closed(50, 90),
              Range.closed(140, 210)),
          // New ranges duplicate
          new ColorRanges(
              Range.closed(40, 50),
              Range.closed(10, 20),
              Range.closed(40, 60))
      );
      // TODO Legendary duplicate
      case "Legendary" -> colorRanges = ImmutableList.of(new ColorRanges(
          Range.closed(225, 255),
          Range.closed(140, 170),
          Range.closed(0, 30)));
      default -> colorRanges = ImmutableList.of(new ColorRanges(
          Range.closed(0, 0),
          Range.closed(0, 0),
          Range.closed(0, 0)));
    }
  }

  /**
   * Parses {@link RarityTypes} from string.
   * @return {@link Optional} with RarityType if successful or Empty
   */
  public static Optional<RarityTypes> parse(String toParse) {
    if (toParse != null) {
      toParse = StringUtils.capitalize(toParse.toLowerCase(Locale.ROOT));
    }
    return Optional.ofNullable(stringValueMap.inverse().getOrDefault(toParse, null));
  }

  /**
   * Obtains {@link RarityTypes} value from image.
   *
   * @param image image to be extract rarity from
   * @return Rarity from {@link RarityTypes}
   */

  @NotNull
  public static RarityTypes computeRarity(@NotNull BufferedImage image) {
    final int variationsCount = 3;
    final List<Integer> widths = new ArrayList<>(variationsCount);
    widths.add((int) Math.round(image.getWidth() * 0.52192)); // 1002
    widths.add((int) Math.round(image.getWidth() * 0.489583)); // 940
    widths.add((int) Math.round(image.getWidth() * 0.489583)); // 940

    final List<Integer> heights = new ArrayList<>(variationsCount);
    heights.add((int) Math.round(image.getHeight() * 0.907407)); // 980
    heights.add((int) Math.round(image.getHeight() * 0.8287)); // 895
    heights.add((int) Math.round(image.getHeight() * 0.912037)); // 985

    RarityTypes computedRarity = RarityTypes.UNKNOWN;
    outer: for (int i = 0; i < variationsCount; i++) {
      Color color = new Color(image.getRGB(widths.get(i), heights.get(i)));
      for (RarityTypes rarity : RarityTypes.values()) {
        if (checkRarityInPixel(rarity, color)) {
          computedRarity = rarity;
        }
        if (computedRarity != RarityTypes.UNKNOWN) {
          break outer;
        }
      }
    }
    return computedRarity;
  }

  /**
   * Checks whether the color's RGB values are contained in the rarity's RGB ranges. For
   * {@link RarityTypes#COMMON} also <br>R == G == B</br> must be true.
   *
   * @return {@code true} if all three RGB values are contained in the rarity, {@code false}
   * otherwise
   */
  private static boolean checkRarityInPixel(RarityTypes rarity, Color color) {
    for (ColorRanges ranges : rarity.getRanges()) {
      if (ranges.red().contains(color.getRed()) && ranges.green().contains(color.getGreen())
          && ranges.blue().contains(color.getBlue())) {
        if (rarity == RarityTypes.COMMON && !checkCommonColorDistance(color)) {
          continue;
        }
        return true;
      }
    }
    return false;
  }

  private static boolean checkCommonColorDistance(Color color) {
    return Math.abs(color.getRed() - color.getGreen()) < MAX_COLOR_DISTANCE
        && Math.abs(color.getRed() - color.getBlue()) < MAX_COLOR_DISTANCE
        && Math.abs(color.getGreen() - color.getBlue()) < MAX_COLOR_DISTANCE;
  }

  @Override
  public String toString() {
    return rarity;
  }

  public ImmutableList<ColorRanges> getRanges() {
    return this.colorRanges;
  }
}
