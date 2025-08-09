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

package io.qrun.qctl.core.cli.cache;


import java.io.IOException;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


/**
 * Implements cache clean operations.
 *
 * Why: Allows users to clear cache artifacts safely (opt-in with --all).
 * @since 0.1.0
 */
@Command(name = "clean", description = "Clean cache")
public class CacheCleanCommand implements Runnable
{
   @Option(names = "--all", description = "Delete entire cache root")
   boolean all;



   /** Executes the clean operation. */
   @Override
   public void run()
   {
      Path root = io.qrun.qctl.core.sys.SystemPaths.cacheDir();
      if(!all)
      {
         System.out.println("Nothing to do. Use --all to remove entire cache.");
         return;
      }
      try
      {
         java.nio.file.Files.walk(root)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(p ->
            {
               try
               {
                  java.nio.file.Files.deleteIfExists(p);
               }
               catch(IOException expected)
               {
                  // no-op, we just want to delete files and directories
               }
            });
         System.out.println("Cache cleared: " + root);
      }
      catch(IOException e)
      {
         System.err.println("cache clean error: " + e.getMessage());
      }
   }
}
