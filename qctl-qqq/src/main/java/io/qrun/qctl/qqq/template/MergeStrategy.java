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
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/*******************************************************************************
 * Handles file conflict resolution during merge operations.
 *
 * @since 0.2.0
 *******************************************************************************/
public class MergeStrategy
{
   private static final DateTimeFormatter BACKUP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

   private final MergeOptions options;
   private final ConsoleUI ui;
   private final FileDiffer differ;

   private Boolean overwriteAll = null;
   private Boolean skipAll = null;



   /***************************************************************************
    * Create a merge strategy with the given options.
    *
    * @param options merge configuration
    * @param ui console UI for interactive prompts
    * @since 0.2.0
    ***************************************************************************/
   public MergeStrategy(MergeOptions options, ConsoleUI ui)
   {
      this.options = options;
      this.ui = ui;
      this.differ = new FileDiffer();
   }



   /***************************************************************************
    * Determine the action for a file during merge.
    *
    * @param relativePath relative path of the file
    * @param targetPath full target path
    * @param newContent the new content to be written
    * @return the merge action to take
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   public MergeAction determineAction(String relativePath, Path targetPath, String newContent)
         throws IOException
   {
      /////////////////////////////////////////////////////////////////////////
      // If merge mode is disabled, always write                             //
      /////////////////////////////////////////////////////////////////////////
      if(!options.enabled())
      {
         return MergeAction.WRITE;
      }

      /////////////////////////////////////////////////////////////////////////
      // Check exclude patterns first                                        //
      /////////////////////////////////////////////////////////////////////////
      if(options.matchesExclude(relativePath))
      {
         return MergeAction.SKIP;
      }

      /////////////////////////////////////////////////////////////////////////
      // If file doesn't exist, always write                                 //
      /////////////////////////////////////////////////////////////////////////
      if(!Files.exists(targetPath))
      {
         return MergeAction.WRITE;
      }

      /////////////////////////////////////////////////////////////////////////
      // File exists - check include patterns                                //
      /////////////////////////////////////////////////////////////////////////
      boolean hasIncludePatterns = options.includePatterns() != null
         && !options.includePatterns().isEmpty();

      if(hasIncludePatterns)
      {
         if(options.matchesInclude(relativePath))
         {
            return handleConflict(relativePath, targetPath, newContent);
         }
         else
         {
            return MergeAction.SKIP;
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // No include patterns - skip existing files by default                //
      /////////////////////////////////////////////////////////////////////////
      return MergeAction.SKIP;
   }



   /***************************************************************************
    * Handle a file conflict (file exists and should be considered).
    *
    * @param relativePath relative path of the file
    * @param targetPath full target path
    * @param newContent the new content
    * @return the merge action
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   private MergeAction handleConflict(String relativePath, Path targetPath, String newContent)
         throws IOException
   {
      /////////////////////////////////////////////////////////////////////////
      // Check if user chose "all" option previously                         //
      /////////////////////////////////////////////////////////////////////////
      if(Boolean.TRUE.equals(overwriteAll))
      {
         return MergeAction.OVERWRITE;
      }
      if(Boolean.TRUE.equals(skipAll))
      {
         return MergeAction.SKIP;
      }

      /////////////////////////////////////////////////////////////////////////
      // Interactive mode - prompt user                                      //
      /////////////////////////////////////////////////////////////////////////
      if(options.interactive())
      {
         return promptForAction(relativePath, targetPath, newContent);
      }

      /////////////////////////////////////////////////////////////////////////
      // Non-interactive with include pattern - overwrite                    //
      /////////////////////////////////////////////////////////////////////////
      return MergeAction.OVERWRITE;
   }



   /***************************************************************************
    * Prompt user for action on conflicting file.
    *
    * @param relativePath relative path of the file
    * @param targetPath full target path
    * @param newContent the new content
    * @return the merge action
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   private MergeAction promptForAction(String relativePath, Path targetPath, String newContent)
         throws IOException
   {
      String existingContent = Files.readString(targetPath);

      while(true)
      {
         ui.println();
         ui.warning("File exists: " + relativePath);
         ui.println("  [o] Overwrite");
         ui.println("  [s] Skip");
         ui.println("  [d] Diff (show changes)");
         ui.println("  [a] Overwrite all remaining");
         ui.println("  [n] Skip all remaining");
         ui.println();

         String choice = ui.promptText("Choice", "s").toLowerCase();

         switch(choice)
         {
            case "o":
               return MergeAction.OVERWRITE;

            case "s":
               return MergeAction.SKIP;

            case "d":
               String diff = differ.diff(existingContent, newContent, relativePath);
               ui.println();
               ui.println(diff);
               break;

            case "a":
               overwriteAll = true;
               return MergeAction.OVERWRITE;

            case "n":
               skipAll = true;
               return MergeAction.SKIP;

            default:
               ui.error("Invalid choice. Please enter o, s, d, a, or n.");
         }
      }
   }



   /***************************************************************************
    * Create a backup of the existing file before overwriting.
    *
    * @param targetPath the file to backup
    * @throws IOException if backup fails
    * @since 0.2.0
    ***************************************************************************/
   public void createBackup(Path targetPath) throws IOException
   {
      if(!options.createBackups() || !Files.exists(targetPath))
      {
         return;
      }

      String timestamp = LocalDateTime.now().format(BACKUP_FORMATTER);
      String backupName = targetPath.getFileName().toString() + "." + timestamp + ".bak";
      Path backupPath = targetPath.resolveSibling(backupName);

      Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
   }



   /***************************************************************************
    * Actions that can be taken for a file during merge.
    *
    * @since 0.2.0
    ***************************************************************************/
   public enum MergeAction
   {
      /*************************************************************************
       * Write the file (new file, no conflict).
       *************************************************************************/
      WRITE,

      /*************************************************************************
       * Overwrite existing file.
       *************************************************************************/
      OVERWRITE,

      /*************************************************************************
       * Skip the file (keep existing).
       *************************************************************************/
      SKIP
   }
}
