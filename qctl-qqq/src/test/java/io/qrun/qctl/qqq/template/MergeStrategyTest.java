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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for MergeStrategy.
 *
 * @since 0.2.0
 *******************************************************************************/
class MergeStrategyTest
{
   @TempDir
   Path tempDir;

   private ConsoleUI ui;



   /***************************************************************************
    * Set up test fixtures.
    *
    * @since 0.2.0
    ***************************************************************************/
   @BeforeEach
   void setUp()
   {
      ui = new ConsoleUI();
   }



   /***************************************************************************
    * Test determineAction with merge disabled always returns WRITE.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_mergeDisabled_alwaysWrites() throws IOException
   {
      MergeOptions options = MergeOptions.disabled();
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path existingFile = tempDir.resolve("existing.txt");
      Files.writeString(existingFile, "existing content");

      MergeStrategy.MergeAction action = strategy.determineAction("existing.txt", existingFile, "new content");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.WRITE);
   }



   /***************************************************************************
    * Test determineAction with new file returns WRITE.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_newFile_writes() throws IOException
   {
      MergeOptions options = MergeOptions.defaults();
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path newFile = tempDir.resolve("new.txt");

      MergeStrategy.MergeAction action = strategy.determineAction("new.txt", newFile, "content");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.WRITE);
   }



   /***************************************************************************
    * Test determineAction with existing file and exclude pattern returns SKIP.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_excludePattern_skips() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of("*.bak"), false, true);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path bakFile = tempDir.resolve("file.bak");
      Files.writeString(bakFile, "backup content");

      MergeStrategy.MergeAction action = strategy.determineAction("file.bak", bakFile, "new");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.SKIP);
   }



   /***************************************************************************
    * Test determineAction with existing file and no include patterns skips.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_existingFile_noInclude_skips() throws IOException
   {
      MergeOptions options = MergeOptions.defaults();
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path existingFile = tempDir.resolve("existing.txt");
      Files.writeString(existingFile, "existing content");

      MergeStrategy.MergeAction action = strategy.determineAction("existing.txt", existingFile, "new");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.SKIP);
   }



   /***************************************************************************
    * Test determineAction with include pattern and non-interactive overwrites.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_includePattern_overwrites() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of("*.java"), List.of(), false, true);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path javaFile = tempDir.resolve("App.java");
      Files.writeString(javaFile, "old code");

      MergeStrategy.MergeAction action = strategy.determineAction("App.java", javaFile, "new code");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.OVERWRITE);
   }



   /***************************************************************************
    * Test determineAction skips file not matching include pattern.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testDetermineAction_includePattern_noMatch_skips() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of("*.java"), List.of(), false, true);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path txtFile = tempDir.resolve("readme.txt");
      Files.writeString(txtFile, "readme content");

      MergeStrategy.MergeAction action = strategy.determineAction("readme.txt", txtFile, "new readme");
      assertThat(action).isEqualTo(MergeStrategy.MergeAction.SKIP);
   }



   /***************************************************************************
    * Test createBackup creates backup file with timestamp.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testCreateBackup_createsBackupFile() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of(), false, true);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path originalFile = tempDir.resolve("original.txt");
      Files.writeString(originalFile, "original content");

      strategy.createBackup(originalFile);

      long backupCount = Files.list(tempDir)
         .filter(p -> p.getFileName().toString().contains(".bak"))
         .count();
      assertThat(backupCount).isEqualTo(1);
   }



   /***************************************************************************
    * Test createBackup with backups disabled does nothing.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testCreateBackup_backupsDisabled_noBackup() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of(), false, false);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path originalFile = tempDir.resolve("original.txt");
      Files.writeString(originalFile, "original content");

      strategy.createBackup(originalFile);

      long backupCount = Files.list(tempDir)
         .filter(p -> p.getFileName().toString().contains(".bak"))
         .count();
      assertThat(backupCount).isEqualTo(0);
   }



   /***************************************************************************
    * Test createBackup with non-existent file does nothing.
    *
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testCreateBackup_nonExistentFile_noBackup() throws IOException
   {
      MergeOptions options = new MergeOptions(true, List.of(), List.of(), false, true);
      MergeStrategy strategy = new MergeStrategy(options, ui);

      Path nonExistent = tempDir.resolve("nonexistent.txt");

      strategy.createBackup(nonExistent);

      long fileCount = Files.list(tempDir).count();
      assertThat(fileCount).isEqualTo(0);
   }
}
