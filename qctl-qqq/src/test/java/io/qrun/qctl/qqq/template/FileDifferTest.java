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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for FileDiffer.
 *
 * @since 0.2.0
 *******************************************************************************/
class FileDifferTest
{
   private FileDiffer differ;



   /***************************************************************************
    * Set up test fixtures.
    *
    * @since 0.2.0
    ***************************************************************************/
   @BeforeEach
   void setUp()
   {
      differ = new FileDiffer();
   }



   /***************************************************************************
    * Test diff of identical files shows no differences.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_identicalContent_noDifferences()
   {
      String content = "line1\nline2\nline3";
      String result = differ.diff(content, content, "test.txt");

      assertThat(result).contains("(no differences)");
   }



   /***************************************************************************
    * Test diff shows added lines.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_addedLines()
   {
      String existing = "line1\nline3";
      String updated = "line1\nline2\nline3";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("+line2");
      assertThat(result).contains("--- existing test.txt");
      assertThat(result).contains("+++ template test.txt");
   }



   /***************************************************************************
    * Test diff shows deleted lines.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_deletedLines()
   {
      String existing = "line1\nline2\nline3";
      String updated = "line1\nline3";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("-line2");
   }



   /***************************************************************************
    * Test diff shows modified lines as delete and add.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_modifiedLines()
   {
      String existing = "hello world";
      String updated = "hello there";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("-hello world");
      assertThat(result).contains("+hello there");
   }



   /***************************************************************************
    * Test diff includes hunk header with line numbers.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_includesHunkHeader()
   {
      String existing = "line1\nold\nline3";
      String updated = "line1\nnew\nline3";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("@@");
   }



   /***************************************************************************
    * Test diff with empty existing file.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_emptyExisting()
   {
      String existing = "";
      String updated = "new content";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("+new content");
   }



   /***************************************************************************
    * Test diff with empty updated file.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_emptyUpdated()
   {
      String existing = "old content";
      String updated = "";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("-old content");
   }



   /***************************************************************************
    * Test diff with multi-line changes.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDiff_multipleChanges()
   {
      String existing = "line1\nline2\nline3\nline4\nline5";
      String updated = "line1\nmodified\nline3\ninserted\nline4\nline5";
      String result = differ.diff(existing, updated, "test.txt");

      assertThat(result).contains("-line2");
      assertThat(result).contains("+modified");
      assertThat(result).contains("+inserted");
   }
}
