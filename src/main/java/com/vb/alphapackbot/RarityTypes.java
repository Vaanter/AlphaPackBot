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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

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

  private static final Map<String, RarityTypes> stringValues = Stream.of(values())
      .collect(Collectors.toMap(RarityTypes::toString, x -> x));
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

  public static Optional<RarityTypes> parse(String toParse) {
    if (toParse != null) {
      toParse = StringUtils.capitalize(toParse.toLowerCase(Locale.ROOT));
    }
    return Optional.ofNullable(stringValues.getOrDefault(toParse, null));
  }

  @Override
  public String toString() {
    return rarity;
  }
}
