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

package io.qrun.qctl.qqq.template;


import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for MergeOptions.
 *
 * @since 0.2.0
 *******************************************************************************/
class MergeOptionsTest
{

   /***************************************************************************
    * Test that disabled() creates options with merge disabled.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDisabled()
   {
      MergeOptions options = MergeOptions.disabled();

      assertThat(options.enabled()).isFalse();
      assertThat(options.includePatterns()).isEmpty();
      assertThat(options.excludePatterns()).isEmpty();
      assertThat(options.interactive()).isFalse();
      assertThat(options.createBackups()).isFalse();
   }



   /***************************************************************************
    * Test that defaults() creates options with merge enabled and backups.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDefaults()
   {
      MergeOptions options = MergeOptions.defaults();

      assertThat(options.enabled()).isTrue();
      assertThat(options.includePatterns()).isEmpty();
      assertThat(options.excludePatterns()).isEmpty();
      assertThat(options.interactive()).isFalse();
      assertThat(options.createBackups()).isTrue();
   }



   /***************************************************************************
    * Test matchesInclude with no patterns (matches all).
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesInclude_noPatterns_matchesAll()
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of(), false, true);

      assertThat(options.matchesInclude("any/file.txt")).isTrue();
      assertThat(options.matchesInclude("path/to/file.java")).isTrue();
   }



   /***************************************************************************
    * Test matchesInclude with simple glob patterns.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesInclude_simpleGlob()
   {
      MergeOptions options = new MergeOptions(true, List.of("*.java"), List.of(), false, true);

      assertThat(options.matchesInclude("App.java")).isTrue();
      assertThat(options.matchesInclude("file.txt")).isFalse();
   }



   /***************************************************************************
    * Test matchesInclude with double-star glob patterns.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesInclude_doubleStarGlob()
   {
      MergeOptions options = new MergeOptions(true, List.of("src/**/*.java"), List.of(), false, true);

      assertThat(options.matchesInclude("src/main/App.java")).isTrue();
      assertThat(options.matchesInclude("src/test/pkg/Test.java")).isTrue();
      assertThat(options.matchesInclude("lib/App.java")).isFalse();
   }



   /***************************************************************************
    * Test matchesExclude with no patterns (excludes none).
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesExclude_noPatterns_excludesNone()
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of(), false, true);

      assertThat(options.matchesExclude("any/file.txt")).isFalse();
      assertThat(options.matchesExclude("path/to/file.java")).isFalse();
   }



   /***************************************************************************
    * Test matchesExclude with patterns.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesExclude_withPatterns()
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of("*.bak", "**/.git/**"), false, true);

      assertThat(options.matchesExclude("file.bak")).isTrue();
      assertThat(options.matchesExclude("dir/.git/config")).isTrue();
      assertThat(options.matchesExclude("file.txt")).isFalse();
   }



   /***************************************************************************
    * Test pattern matching with question mark wildcard.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesInclude_questionMarkWildcard()
   {
      MergeOptions options = new MergeOptions(true, List.of("file?.txt"), List.of(), false, true);

      assertThat(options.matchesInclude("file1.txt")).isTrue();
      assertThat(options.matchesInclude("fileA.txt")).isTrue();
      assertThat(options.matchesInclude("file.txt")).isFalse();
      assertThat(options.matchesInclude("file12.txt")).isFalse();
   }



   /***************************************************************************
    * Test multiple include patterns (any match succeeds).
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testMatchesInclude_multiplePatterns()
   {
      MergeOptions options = new MergeOptions(true, List.of("*.java", "*.xml"), List.of(), false, true);

      assertThat(options.matchesInclude("App.java")).isTrue();
      assertThat(options.matchesInclude("pom.xml")).isTrue();
      assertThat(options.matchesInclude("file.txt")).isFalse();
   }
}
