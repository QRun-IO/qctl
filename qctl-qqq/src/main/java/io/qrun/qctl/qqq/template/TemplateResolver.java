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


/*******************************************************************************
 * Resolves template sources to local paths.
 *
 * Supports: local paths, git URLs, github.com/org/repo shorthand.
 *
 * @since 0.1.0
 *******************************************************************************/
public class TemplateResolver
{
   private static final String GITHUB_PREFIX = "github.com/";
   private static final String GIT_SUFFIX = ".git";



   /***************************************************************************
    * Resolve a template source to a local directory path.
    *
    * @param source template source (path, git URL, or github shorthand)
    * @param version optional version (semver tag)
    * @return local path to template directory
    * @throws IOException if template cannot be resolved
    * @since 0.1.0
    ***************************************************************************/
   public Path resolve(String source, String version) throws IOException
   {
      // Local path
      Path localPath = Path.of(source);
      if(Files.isDirectory(localPath))
      {
         System.out.println("Using local template: " + localPath.toAbsolutePath());
         return localPath.toAbsolutePath();
      }

      // Git URL or GitHub shorthand
      String gitUrl = toGitUrl(source);
      return cloneTemplate(gitUrl, version);
   }



   /***************************************************************************
    * Convert source to a full git URL.
    *
    * @param source template source
    * @return git clone URL
    * @since 0.1.0
    ***************************************************************************/
   private String toGitUrl(String source)
   {
      // Already a git URL
      if(source.startsWith("git@") || source.startsWith("https://") || source.startsWith("git://"))
      {
         return source;
      }

      // GitHub shorthand: github.com/org/repo or just org/repo
      if(source.startsWith(GITHUB_PREFIX))
      {
         String repo = source.substring(GITHUB_PREFIX.length());
         return "https://github.com/" + repo + (repo.endsWith(GIT_SUFFIX) ? "" : GIT_SUFFIX);
      }

      // Assume GitHub shorthand if it looks like org/repo
      if(source.matches("[\\w.-]+/[\\w.-]+"))
      {
         return "https://github.com/" + source + GIT_SUFFIX;
      }

      throw new IllegalArgumentException("Invalid template source: " + source);
   }



   /***************************************************************************
    * Clone a git repository to a temporary directory.
    *
    * @param gitUrl git clone URL
    * @param version optional version tag
    * @return path to cloned template
    * @throws IOException if clone fails
    * @since 0.1.0
    ***************************************************************************/
   private Path cloneTemplate(String gitUrl, String version) throws IOException
   {
      Path tempDir = Files.createTempDirectory("qctl-template-");
      System.out.println("Cloning: " + gitUrl);

      try
      {
         ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1");
         if(version != null && !version.isBlank())
         {
            pb.command().add("--branch");
            pb.command().add(version.startsWith("v") ? version : "v" + version);
         }
         pb.command().add(gitUrl);
         pb.command().add(tempDir.toString());
         pb.inheritIO();

         Process process = pb.start();
         int     exitCode = process.waitFor();
         if(exitCode != 0)
         {
            throw new IOException("git clone failed with exit code " + exitCode);
         }
         return tempDir;
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new IOException("git clone interrupted", e);
      }
   }
}
