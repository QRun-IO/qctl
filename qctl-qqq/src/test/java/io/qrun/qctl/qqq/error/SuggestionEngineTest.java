/*
 * All Rights Reserved
 *
 * Copyright (c) 2025. QRunIO.   Contact: contact@qrun.io
 *
 * THE CONTENTS OF THIS PROJECT ARE PROPRIETARY AND CONFIDENTIAL.
 * UNAUTHORIZED COPYING, TRANSFERRING, OR REPRODUCTION OF ANY PART OF THIS PROJECT, VIA ANY MEDIUM, IS STRICTLY PROHIBITED.
 *
 * The receipt or possession of the source code and/or any parts thereof does not convey or imply any right to use them
 * for any purpose other than the purpose for which they were provided to you.
 */

package io.qrun.qctl.qqq.error;


import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Tests for SuggestionEngine fuzzy matching.
 *
 * @since 0.2.0
 *******************************************************************************/
class SuggestionEngineTest
{
   private SuggestionEngine engine;
   private List<String> candidates;



   @BeforeEach
   void setUp()
   {
      engine = new SuggestionEngine();
      candidates = List.of(
         "new-qqq-application",
         "new-qqq-etl",
         "new-qqq-api",
         "spring-boot-starter",
         "react-dashboard"
      );
   }



   /***************************************************************************
    * Test exact match returns first in results.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_exactMatch_returnsFirst()
   {
      List<String> results = engine.findSimilar("new-qqq-application", candidates);
      assertThat(results).isNotEmpty();
      assertThat(results.get(0)).isEqualTo("new-qqq-application");
   }



   /***************************************************************************
    * Test typo with one character difference.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_typoOneChar_suggestsCorrect()
   {
      List<String> results = engine.findSimilar("new-qqq-aplication", candidates);
      assertThat(results).contains("new-qqq-application");
   }



   /***************************************************************************
    * Test prefix match gives bonus.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_prefixMatch_suggestsMatches()
   {
      List<String> results = engine.findSimilar("new-qqq-ap", candidates);
      assertThat(results).contains("new-qqq-api");
   }



   /***************************************************************************
    * Test missing hyphen typo.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_missingHyphen_suggests()
   {
      List<String> results = engine.findSimilar("newqqq-application", candidates);
      assertThat(results).contains("new-qqq-application");
   }



   /***************************************************************************
    * Test wrong word order returns nothing for distant strings.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_veryDifferent_returnsEmpty()
   {
      List<String> results = engine.findSimilar("completely-different-name", candidates);
      assertThat(results).isEmpty();
   }



   /***************************************************************************
    * Test null input returns empty.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_nullInput_returnsEmpty()
   {
      List<String> results = engine.findSimilar(null, candidates);
      assertThat(results).isEmpty();
   }



   /***************************************************************************
    * Test empty input returns empty.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_emptyInput_returnsEmpty()
   {
      List<String> results = engine.findSimilar("", candidates);
      assertThat(results).isEmpty();
   }



   /***************************************************************************
    * Test null candidates returns empty.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_nullCandidates_returnsEmpty()
   {
      List<String> results = engine.findSimilar("test", null);
      assertThat(results).isEmpty();
   }



   /***************************************************************************
    * Test empty candidates returns empty.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_emptyCandidates_returnsEmpty()
   {
      List<String> results = engine.findSimilar("test", List.of());
      assertThat(results).isEmpty();
   }



   /***************************************************************************
    * Test case insensitive matching.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_caseInsensitive_suggestsMatch()
   {
      List<String> results = engine.findSimilar("NEW-QQQ-APPLICATION", candidates);
      assertThat(results).contains("new-qqq-application");
   }



   /***************************************************************************
    * Test limits suggestions to 3.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_limitsSuggestions()
   {
      List<String> manyCandidates = List.of(
         "test1", "test2", "test3", "test4", "test5"
      );
      List<String> results = engine.findSimilar("test", manyCandidates);
      assertThat(results).hasSizeLessThanOrEqualTo(3);
   }



   /***************************************************************************
    * Test transposed characters.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_transposedChars_suggests()
   {
      List<String> results = engine.findSimilar("new-qqq-applictaion", candidates);
      assertThat(results).contains("new-qqq-application");
   }



   /***************************************************************************
    * Test short strings with similar candidates.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFindSimilar_shortCandidates_suggests()
   {
      List<String> shortCandidates = List.of("api", "etl", "web", "app");
      List<String> results = engine.findSimilar("aip", shortCandidates);
      assertThat(results).contains("api");
   }
}
