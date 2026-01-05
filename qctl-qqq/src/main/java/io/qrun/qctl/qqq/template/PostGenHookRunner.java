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
import java.nio.file.Path;
import java.util.List;


/*******************************************************************************
 * Runs post-generation hooks after template rendering.
 *
 * Executes commands like mvn compile, git init, npm install.
 *
 * @since 0.1.0
 *******************************************************************************/
public class PostGenHookRunner
{
   private static final String ANSI_BOLD = "\u001B[1m";
   private static final String ANSI_GREEN = "\u001B[32m";
   private static final String ANSI_RED = "\u001B[31m";
   private static final String ANSI_RESET = "\u001B[0m";



   /***************************************************************************
    * Run post-generation hooks.
    *
    * @param projectDir generated project directory
    * @param hooks list of hooks to run
    * @throws IOException if a required hook fails
    * @since 0.1.0
    ***************************************************************************/
   public void run(Path projectDir, List<TemplateManifest.PostGenHook> hooks)
         throws IOException
   {
      System.out.println("\n" + ANSI_BOLD + "Running post-generation hooks:" + ANSI_RESET);

      for(TemplateManifest.PostGenHook hook : hooks)
      {
         runHook(projectDir, hook);
      }
   }



   /***************************************************************************
    * Run a single hook.
    *
    * @param projectDir project directory
    * @param hook hook definition
    * @throws IOException if required hook fails
    * @since 0.1.0
    ***************************************************************************/
   private void runHook(Path projectDir, TemplateManifest.PostGenHook hook)
         throws IOException
   {
      String  name = hook.name() != null ? hook.name() : hook.command();
      boolean optional = hook.optional() != null && hook.optional();

      System.out.println("\n  " + ANSI_BOLD + name + ANSI_RESET);
      System.out.println("  $ " + hook.command());

      Path workDir = projectDir;
      if(hook.workDir() != null && !hook.workDir().isBlank())
      {
         workDir = projectDir.resolve(hook.workDir());
      }

      try
      {
         ProcessBuilder pb = createProcessBuilder(hook.command());
         pb.directory(workDir.toFile());
         pb.inheritIO();

         Process process = pb.start();
         int     exitCode = process.waitFor();

         if(exitCode == 0)
         {
            System.out.println("  " + ANSI_GREEN + "OK" + ANSI_RESET);
         }
         else
         {
            System.out.println("  " + ANSI_RED + "FAILED (exit " + exitCode + ")" + ANSI_RESET);
            if(!optional)
            {
               throw new IOException("Hook failed: " + name);
            }
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new IOException("Hook interrupted: " + name, e);
      }
   }



   /***************************************************************************
    * Create a ProcessBuilder for a command.
    *
    * @param command command string
    * @return process builder
    * @since 0.1.0
    ***************************************************************************/
   private ProcessBuilder createProcessBuilder(String command)
   {
      String os = System.getProperty("os.name").toLowerCase();
      if(os.contains("win"))
      {
         return new ProcessBuilder("cmd", "/c", command);
      }
      else
      {
         return new ProcessBuilder("sh", "-c", command);
      }
   }
}
