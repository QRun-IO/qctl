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


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import picocli.CommandLine.Command;


/**
 * Lists cache entries (limited depth and count) for quick inspection.
 */
@Command(name = "ls", description = "List cache entries")
public class CacheLsCommand implements Runnable
{
   /** Executes the list operation. */
   @Override
   public void run()
   {
      Path root = io.qrun.qctl.core.sys.SystemPaths.cacheDir();
      try(Stream<Path> s = Files.walk(root, 2))
      {
         s.filter(Files::isRegularFile).limit(50).forEach(p -> System.out.println(root.relativize(p)));
      }
      catch(Exception e)
      {
         System.err.println("cache ls error: " + e.getMessage());
      }
   }
}
