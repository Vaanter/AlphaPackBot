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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RarityTypesTest {

  @Test
  public void testComputeRarity_CommonOldFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Common_old_full.png"));
    Assertions.assertEquals(RarityTypes.COMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_UncommonOldFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Uncommon_old_full.png"));
    Assertions.assertEquals(RarityTypes.UNCOMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareOldFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_old_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareOldDupFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_old_dup_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareOldDupFullStretched_unknown() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Rare_old_dup_full_stretched.png"));
    // Result from stretched images is undefined
    Assertions.assertTrue(
        List.of(RarityTypes.UNKNOWN, RarityTypes.RARE).contains(RarityTypes.computeRarity(image)));
  }

  @Test
  public void testComputeRarity_RareOldNewUiFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_old_newUI_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareOldNewUiFullStretched_unknown() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Rare_old_newUI_full_stretched.png"));
    // Result from stretched images is undefined
    Assertions.assertTrue(
        List.of(RarityTypes.UNKNOWN, RarityTypes.RARE).contains(RarityTypes.computeRarity(image)));
  }

  @Test
  public void testComputeRarity_EpicOldFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Epic_old_full.png"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryOldFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Legendary_old_full.png"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_CommonNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Common_new_full.png"));
    Assertions.assertEquals(RarityTypes.COMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_CommonNewDupFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Common_new_dup_full.png"));
    Assertions.assertEquals(RarityTypes.COMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_UncommonNewNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Uncommon_newnew_full.png"));
    Assertions.assertEquals(RarityTypes.UNCOMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_UncommonNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Uncommon_new_full.png"));
    Assertions.assertEquals(RarityTypes.UNCOMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_UncommonNewDupFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Uncommon_new_dup_full.png"));
    Assertions.assertEquals(RarityTypes.UNCOMMON, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareNewNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_newnew_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_new_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_RareNewDupFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Rare_new_dup_full.png"));
    Assertions.assertEquals(RarityTypes.RARE, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_EpicNewNewFull() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Epic_newnew_full.png"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_EpicNewNewFull2() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Epic_newnew_full2.jpg"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_EpicNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Epic_new_full.png"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_EpicNewDupFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Epic_new_dup_full.jpg"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_EpicNewFull720p() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Epic_new_full_720p.jpg"));
    Assertions.assertEquals(RarityTypes.EPIC, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryNewNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Legendary_newnew_full.png"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryNewNewSearchFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Legendary_newnew_search_full.png"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryNewFull() throws IOException {
    BufferedImage image = ImageIO.read(new File("src/test/resources/Legendary_new_full.png"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryNewFullChinese() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Legendary_new_full_chinese.jpg"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }

  @Test
  public void testComputeRarity_LegendaryNewNewFullChinese() throws IOException {
    BufferedImage image =
        ImageIO.read(new File("src/test/resources/Legendary_newnew_full_chinese.jpg"));
    Assertions.assertEquals(RarityTypes.LEGENDARY, RarityTypes.computeRarity(image));
  }
}
