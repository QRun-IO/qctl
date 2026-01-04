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


/*******************************************************************************
 * Configuration options for merge mode during template rendering.
 *
 * @param enabled whether merge mode is active
 * @param includePatterns glob patterns for files to include (overwrite if exist)
 * @param excludePatterns glob patterns for files to exclude (never overwrite)
 * @param interactive whether to prompt for each conflicting file
 * @param createBackups whether to create backups before overwriting
 * @since 0.2.0
 *******************************************************************************/
public record MergeOptions(
   boolean enabled,
   List<String> includePatterns,
   List<String> excludePatterns,
   boolean interactive,
   boolean createBackups
)
{

   /***************************************************************************
    * Create disabled merge options (normal overwrite behavior).
    *
    * @return disabled merge options
    * @since 0.2.0
    ***************************************************************************/
   public static MergeOptions disabled()
   {
      return new MergeOptions(false, List.of(), List.of(), false, false);
   }



   /***************************************************************************
    * Create default merge options (skip existing, create backups).
    *
    * @return default merge options
    * @since 0.2.0
    ***************************************************************************/
   public static MergeOptions defaults()
   {
      return new MergeOptions(true, List.of(), List.of(), false, true);
   }



   /***************************************************************************
    * Check if a file path matches the include patterns.
    *
    * @param relativePath the relative file path to check
    * @return true if the file matches include patterns (or no patterns defined)
    * @since 0.2.0
    ***************************************************************************/
   public boolean matchesInclude(String relativePath)
   {
      if(includePatterns == null || includePatterns.isEmpty())
      {
         return true;
      }
      return matchesAnyPattern(relativePath, includePatterns);
   }



   /***************************************************************************
    * Check if a file path matches the exclude patterns.
    *
    * @param relativePath the relative file path to check
    * @return true if the file matches exclude patterns
    * @since 0.2.0
    ***************************************************************************/
   public boolean matchesExclude(String relativePath)
   {
      if(excludePatterns == null || excludePatterns.isEmpty())
      {
         return false;
      }
      return matchesAnyPattern(relativePath, excludePatterns);
   }



   /***************************************************************************
    * Check if a file path matches any of the given glob patterns.
    *
    * @param relativePath the relative file path to check
    * @param patterns the glob patterns to match against
    * @return true if the file matches any pattern
    * @since 0.2.0
    ***************************************************************************/
   private boolean matchesAnyPattern(String relativePath, List<String> patterns)
   {
      for(String pattern : patterns)
      {
         if(matchesGlob(relativePath, pattern))
         {
            return true;
         }
      }
      return false;
   }



   /***************************************************************************
    * Match a path against a glob pattern.
    *
    * @param path the path to check
    * @param pattern the glob pattern
    * @return true if the path matches
    * @since 0.2.0
    ***************************************************************************/
   private boolean matchesGlob(String path, String pattern)
   {
      /////////////////////////////////////////////////////////////////////////
      // Convert glob to regex:                                              //
      // ** matches any path segments                                        //
      // * matches any characters except path separator                      //
      // ? matches single character                                          //
      /////////////////////////////////////////////////////////////////////////
      String regex = pattern
         .replace(".", "\\.")
         .replace("**", "<<<DOUBLESTAR>>>")
         .replace("*", "[^/]*")
         .replace("<<<DOUBLESTAR>>>", ".*")
         .replace("?", ".");

      return path.matches(regex);
   }
}
