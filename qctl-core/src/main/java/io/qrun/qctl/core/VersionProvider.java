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

package io.qrun.qctl.core;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;


/*******************************************************************************
 * Provides version information for the CLI from build-time properties.
 *
 * Reads version.properties populated by Maven resource filtering.
 *
 * @since 0.1.0
 *******************************************************************************/
public class VersionProvider implements IVersionProvider
{
   private static final String VERSION_PROPERTIES = "/version.properties";


   /***************************************************************************
    * Returns version strings for display by picocli --version flag.
    *
    * @return array of version strings
    * @since 0.1.0
    ***************************************************************************/
   @Override
   public String[] getVersion()
   {
      Properties props = new Properties();
      try(InputStream is = getClass().getResourceAsStream(VERSION_PROPERTIES))
      {
         if(is != null)
         {
            props.load(is);
         }
      }
      catch(IOException e)
      {
         // Fall back to defaults
      }

      String version = props.getProperty("version", "unknown");
      String buildTime = props.getProperty("build.time", "");
      String gitCommit = props.getProperty("git.commit", "");

      if(!gitCommit.isEmpty())
      {
         return new String[] {
            "qctl " + version,
            "Build: " + buildTime,
            "Commit: " + gitCommit
         };
      }
      else if(!buildTime.isEmpty())
      {
         return new String[] {
            "qctl " + version,
            "Build: " + buildTime
         };
      }
      else
      {
         return new String[] { "qctl " + version };
      }
   }
}
