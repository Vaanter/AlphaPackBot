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
  private final String rarity;
  private final ImmutableList<ImmutableList<Range<Integer>>> range;

  RarityTypes(@NotNull String rarity) {
    this.rarity = rarity;
    switch (rarity) {
      case "Common" -> range = ImmutableList.of(
          // Old ranges
          ImmutableList.of(
              Range.closed(90, 100),
              Range.closed(90, 100),
              Range.closed(90, 100)),
          // New ranges
          ImmutableList.of(
              Range.closed(160, 180),
              Range.closed(160, 180),
              Range.closed(160, 180)),
          // New ranges duplicate
          ImmutableList.of(
              Range.closed(60, 80),
              Range.closed(60, 80),
              Range.closed(60, 80))
      );
      case "Uncommon" -> range = ImmutableList.of(
          // Old ranges
          ImmutableList.of(
              Range.closed(200, 250),
              Range.closed(185, 235),
              Range.closed(160, 210)),
          // New ranges duplicate
          ImmutableList.of(
              Range.closed(40, 55),
              Range.closed(75, 90),
              Range.closed(50, 60)),
          // New ranges
          ImmutableList.of(
              Range.closed(85, 95),
              Range.closed(210, 220),
              Range.closed(135, 150))
      );
      case "Rare" -> range = ImmutableList.of(
          // New ranges
          ImmutableList.of(
              Range.closed(55, 105),
              Range.closed(135, 200),
              Range.closed(185, 255)),
          // New ranges duplicate
          ImmutableList.of(
              Range.closed(30, 40),
              Range.closed(60, 70),
              Range.closed(75, 90))
      );
      case "Epic" -> range = ImmutableList.of(
          // New ranges
          ImmutableList.of(
              Range.closed(135, 185),
              Range.closed(50, 90),
              Range.closed(155, 210)),
          // New ranges duplicate
          ImmutableList.of(
              Range.closed(40, 50),
              Range.closed(10, 20),
              Range.closed(40, 60))
      );
      case "Legendary" -> range = ImmutableList.of(ImmutableList.of(
          Range.closed(230, 255),
          Range.closed(145, 170),
          Range.closed(5, 30)));
      default -> range = ImmutableList.of(ImmutableList.of(
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
    int width = (int) Math.round(image.getWidth() * 0.489583); // ~940 @ FHD
    int height = (int) Math.round(image.getHeight() * 0.912037); // ~985 @ FHD
    Color color = new Color(image.getRGB(width, height));
    int[] colors = {color.getRed(), color.getGreen(), color.getBlue()};
    RarityTypes computedRarity = RarityTypes.UNKNOWN;
    for (RarityTypes rarity : RarityTypes.values()) {
      if (checkRarityInPixel(rarity, colors)) {
        computedRarity = rarity;
      }
      if (computedRarity != RarityTypes.UNKNOWN) {
        break;
      }
    }
    // Old packs were checked from pixel at different height
    if (computedRarity == RarityTypes.UNKNOWN) {
      height = (int) Math.round(image.getHeight() * 0.83333); // ~900 @ FHD
      color = new Color(image.getRGB(width, height));
      colors = new int[]{color.getRed(), color.getGreen(), color.getBlue()};
      for (RarityTypes rarity : RarityTypes.values()) {
        if (checkRarityInPixel(rarity, colors)) {
          computedRarity = rarity;
        }
        if (computedRarity != RarityTypes.UNKNOWN) {
          break;
        }
      }
    }
    return computedRarity;
  }

  /**
   * Checks whether the colors match the rarity's ranges.
   * @return true colors match the rarity, false otherwise
   */
  private static boolean checkRarityInPixel(RarityTypes rarity, int[] colors) {
    for (var ranges: rarity.getRanges()) {
      int hitCounter = 0;
      // Loop through color ranges checking for match
      for (int i = 0; i < 3; i++) {
        if (ranges.get(i).contains(colors[i])) {
          hitCounter += 1;
        }
      }
      if (hitCounter == 3) {
        // If rarity is common, all colors must match
        if (rarity == RarityTypes.COMMON && (colors[0] != colors[1] || colors[0] != colors[2])) {
          continue;
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return rarity;
  }

  public ImmutableList<ImmutableList<Range<Integer>>> getRanges() {
    return this.range;
  }
}
