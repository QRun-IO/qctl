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

package io.qrun.qctl.e2e.harness;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import io.qrun.qctl.core.Main;
import io.qrun.qctl.shared.spi.CommandPlugin;
import picocli.CommandLine;


/*******************************************************************************
 * Test harness for executing qctl commands in-process.
 *
 * Captures stdout, stderr, and exit codes for assertion. This enables fast,
 * deterministic E2E tests without spawning separate processes.
 *
 * @since 0.2.0
 *******************************************************************************/
public class CommandTestHarness
{
   private final ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
   private final ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
   private int exitCode = -1;



   /***************************************************************************
    * Execute a qctl command with the given arguments.
    *
    * @param args command-line arguments (e.g., "qqq", "init", "my-template")
    * @return this harness for fluent assertions
    * @since 0.2.0
    ***************************************************************************/
   public CommandTestHarness execute(String... args)
   {
      PrintStream origOut = System.out;
      PrintStream origErr = System.err;

      try
      {
         System.setOut(new PrintStream(stdoutStream, true, StandardCharsets.UTF_8));
         System.setErr(new PrintStream(stderrStream, true, StandardCharsets.UTF_8));

         CommandLine cmd = buildCommandLine();
         exitCode = cmd.execute(args);
      }
      finally
      {
         System.setOut(origOut);
         System.setErr(origErr);
      }

      return this;
   }



   /***************************************************************************
    * Build CommandLine with all SPI-discovered plugins.
    *
    * @return configured CommandLine instance
    * @since 0.2.0
    ***************************************************************************/
   private CommandLine buildCommandLine()
   {
      CommandLine cmd = new CommandLine(new Main());

      //////////////////////////////////////////////////////////////////////////
      // Discover and register subcommands via SPI                            //
      //////////////////////////////////////////////////////////////////////////
      try
      {
         java.util.ServiceLoader<CommandPlugin> loader =
            java.util.ServiceLoader.load(CommandPlugin.class);
         for(CommandPlugin plugin : loader)
         {
            Object      command = plugin.getCommand();
            CommandLine sub     = new CommandLine(command);
            String      name    = sub.getCommandSpec().name();
            cmd.addSubcommand(name, command);
         }
      }
      catch(java.util.ServiceConfigurationError | NoClassDefFoundError e)
      {
         //////////////////////////////////////////////////////////////////
         // Log but continue - some plugins may not be available        //
         //////////////////////////////////////////////////////////////////
         System.err.println("warning: some command plugins could not be loaded: " + e.getMessage());
      }

      return cmd;
   }



   /***************************************************************************
    * Get the captured stdout content.
    *
    * @return stdout as string
    * @since 0.2.0
    ***************************************************************************/
   public String getStdout()
   {
      return stdoutStream.toString(StandardCharsets.UTF_8);
   }



   /***************************************************************************
    * Get the captured stderr content.
    *
    * @return stderr as string
    * @since 0.2.0
    ***************************************************************************/
   public String getStderr()
   {
      return stderrStream.toString(StandardCharsets.UTF_8);
   }



   /***************************************************************************
    * Get the exit code from the last command execution.
    *
    * @return exit code (0 = success)
    * @since 0.2.0
    ***************************************************************************/
   public int getExitCode()
   {
      return exitCode;
   }



   /***************************************************************************
    * Check if stdout contains a specific string.
    *
    * @param expected the string to search for
    * @return true if stdout contains the string
    * @since 0.2.0
    ***************************************************************************/
   public boolean stdoutContains(String expected)
   {
      return getStdout().contains(expected);
   }



   /***************************************************************************
    * Check if stderr contains a specific string.
    *
    * @param expected the string to search for
    * @return true if stderr contains the string
    * @since 0.2.0
    ***************************************************************************/
   public boolean stderrContains(String expected)
   {
      return getStderr().contains(expected);
   }



   /***************************************************************************
    * Reset the harness for reuse.
    *
    * @return this harness for fluent chaining
    * @since 0.2.0
    ***************************************************************************/
   public CommandTestHarness reset()
   {
      stdoutStream.reset();
      stderrStream.reset();
      exitCode = -1;
      return this;
   }
}
