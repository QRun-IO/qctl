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

package io.qrun.qctl.e2e;


import io.qrun.qctl.e2e.harness.CommandTestHarness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.qrun.qctl.e2e.harness.ExitCodeAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Integration tests for the qqq init command.
 *
 * Tests command parsing, help text, and basic validation without
 * spawning native processes or making network calls.
 *
 * @since 0.2.0
 *******************************************************************************/
class InitCommandIT
{
   private CommandTestHarness harness;



   @BeforeEach
   void setUp()
   {
      harness = new CommandTestHarness();
   }



   /***************************************************************************
    * Test that --help displays init command usage.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_help_showsUsage()
   {
      harness.execute("qqq", "init", "--help");

      assertSuccess(harness);
      assertThat(harness.getStdout())
         .contains("Initialize a new project from a template")
         .contains("--output")
         .contains("--force")
         .contains("--dry-run")
         .contains("--merge");
   }



   /***************************************************************************
    * Test that qqq command group shows help.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testQqq_help_showsSubcommands()
   {
      harness.execute("qqq", "--help");

      assertSuccess(harness);
      assertThat(harness.getStdout())
         .contains("init")
         .contains("list");
   }



   /***************************************************************************
    * Test that top-level --version works.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testVersion_showsVersionInfo()
   {
      harness.execute("--version");

      assertSuccess(harness);
      assertThat(harness.getStdout())
         .containsPattern("qctl.*\\d+\\.\\d+\\.\\d+");
   }



   /***************************************************************************
    * Test that unknown command shows error.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testUnknownCommand_showsError()
   {
      harness.execute("unknown-command");

      assertThat(harness.getExitCode()).isNotZero();
      assertThat(harness.getStderr())
         .contains("unknown-command");
   }



   /***************************************************************************
    * Test that qqq list runs and shows templates.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testList_showsTemplates()
   {
      harness.execute("qqq", "list");

      assertSuccess(harness);
      assertThat(harness.getStdout())
         .containsIgnoringCase("Available Templates");
   }
}
