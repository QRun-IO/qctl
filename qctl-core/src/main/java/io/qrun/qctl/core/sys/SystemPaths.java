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

package io.qrun.qctl.core.sys;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * OS-specific configuration and cache directory helpers.
 *
 * Why: Provide predictable config/cache locations across platforms.
 * @since 0.1.0
 */
public final class SystemPaths
{
   /***************************************************************************
    * Non-instantiable utility class.
    *
    * @since 0.1.0
    ***************************************************************************/
   private SystemPaths()
   {
   }



   /** Returns the user configuration directory for qctl. */
   public static Path configDir()
   {
      String os = System.getProperty("os.name").toLowerCase();
      if(os.contains("win"))
      {
         String appData = System.getenv("APPDATA");
         if(appData != null && !appData.isEmpty())
         {
            return Paths.get(appData, "qctl");
         }
         return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "qctl");
      }
      else if(os.contains("mac"))
      {
         return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "qctl");
      }
      else
      {
         String xdg = System.getenv("XDG_CONFIG_HOME");
         if(xdg != null && !xdg.isEmpty())
         {
            return Paths.get(xdg, "qctl");
         }
         return Paths.get(System.getProperty("user.home"), ".config", "qctl");
      }
   }



   /** Returns the user cache directory for qctl. */
   public static Path cacheDir()
   {
      String os = System.getProperty("os.name").toLowerCase();
      if(os.contains("win"))
      {
         String local = System.getenv("LOCALAPPDATA");
         if(local != null && !local.isEmpty())
         {
            return Paths.get(local, "qctl", "cache");
         }
         return Paths.get(System.getProperty("user.home"), "AppData", "Local", "qctl", "cache");
      }
      else if(os.contains("mac"))
      {
         return Paths.get(System.getProperty("user.home"), "Library", "Caches", "qctl");
      }
      else
      {
         String xdg = System.getenv("XDG_CACHE_HOME");
         if(xdg != null && !xdg.isEmpty())
         {
            return Paths.get(xdg, "qctl");
         }
         return Paths.get(System.getProperty("user.home"), ".cache", "qctl");
      }
   }



   /** Ensures the given directory exists, creating it if needed. */
   public static Path ensureDir(Path p)
   {
      try
      {
         Files.createDirectories(p);
      }
      catch(Exception expected)
      {
      }
      return p;
   }
}
