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


import io.qrun.qctl.shared.ExitCodes;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * AssertJ-style assertions for CommandTestHarness exit codes.
 *
 * Provides readable assertions for common exit code scenarios.
 *
 * @since 0.2.0
 *******************************************************************************/
public final class ExitCodeAssertions
{


   /***************************************************************************
    * Private constructor - utility class.
    *
    * @since 0.2.0
    ***************************************************************************/
   private ExitCodeAssertions()
   {
   }



   /***************************************************************************
    * Assert the command executed successfully (exit code 0).
    *
    * @param harness the test harness
    * @since 0.2.0
    ***************************************************************************/
   public static void assertSuccess(CommandTestHarness harness)
   {
      assertThat(harness.getExitCode())
         .as("Expected success (exit code 0), got %d.\nStdout: %s\nStderr: %s",
            harness.getExitCode(), harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);
   }



   /***************************************************************************
    * Assert the command failed with a generic error (exit code 1).
    *
    * @param harness the test harness
    * @since 0.2.0
    ***************************************************************************/
   public static void assertGenericError(CommandTestHarness harness)
   {
      assertThat(harness.getExitCode())
         .as("Expected generic error (exit code 1)")
         .isEqualTo(ExitCodes.GENERIC);
   }



   /***************************************************************************
    * Assert the command failed with a usage error (exit code 2).
    *
    * @param harness the test harness
    * @since 0.2.0
    ***************************************************************************/
   public static void assertUsageError(CommandTestHarness harness)
   {
      assertThat(harness.getExitCode())
         .as("Expected usage error (exit code 2)")
         .isEqualTo(ExitCodes.USAGE);
   }



   /***************************************************************************
    * Assert the command failed with a validation error (exit code 6).
    *
    * @param harness the test harness
    * @since 0.2.0
    ***************************************************************************/
   public static void assertValidationError(CommandTestHarness harness)
   {
      assertThat(harness.getExitCode())
         .as("Expected validation error (exit code 6)")
         .isEqualTo(ExitCodes.VALIDATION);
   }



   /***************************************************************************
    * Assert the command failed with a conflict error (exit code 8).
    *
    * @param harness the test harness
    * @since 0.2.0
    ***************************************************************************/
   public static void assertConflictError(CommandTestHarness harness)
   {
      assertThat(harness.getExitCode())
         .as("Expected conflict error (exit code 8)")
         .isEqualTo(ExitCodes.CONFLICT);
   }



   /***************************************************************************
    * Assert the command returned a specific exit code.
    *
    * @param harness the test harness
    * @param expectedCode expected exit code
    * @since 0.2.0
    ***************************************************************************/
   public static void assertExitCode(CommandTestHarness harness, int expectedCode)
   {
      assertThat(harness.getExitCode())
         .as("Expected exit code %d", expectedCode)
         .isEqualTo(expectedCode);
   }
}
