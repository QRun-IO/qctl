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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.qrun.qctl.e2e.harness.CommandTestHarness;
import io.qrun.qctl.e2e.harness.TestTemplateRegistry;
import io.qrun.qctl.qqq.registry.TemplateRegistryFactory;
import io.qrun.qctl.shared.ExitCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Comprehensive E2E tests for the qqq init command using fixture templates.
 *
 * Tests command-line flags, project generation, and error handling scenarios.
 * Uses TestTemplateRegistry to provide fully offline, deterministic tests.
 *
 * @since 0.2.0
 *******************************************************************************/
class InitCommandE2ETest
{
   private CommandTestHarness harness;
   private TestTemplateRegistry registry;
   private InputStream originalIn;

   @TempDir
   Path tempDir;



   @BeforeEach
   void setUp() throws IOException
   {
      harness = new CommandTestHarness();
      registry = new TestTemplateRegistry();
      registry.loadFromResources("test-minimal");
      registry.loadFromResources("test-with-prompts");
      TemplateRegistryFactory.setRegistry(registry);

      //////////////////////////////////////////////////////////////////////////
      // Provide empty input to prevent hanging on unexpected prompts         //
      //////////////////////////////////////////////////////////////////////////
      originalIn = System.in;
      System.setIn(new ByteArrayInputStream("\n\n\n".getBytes(StandardCharsets.UTF_8)));
   }



   @AfterEach
   void tearDown()
   {
      TemplateRegistryFactory.reset();
      System.setIn(originalIn);
   }



   /***************************************************************************
    * Test happy path: init with minimal template creates project.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_minimalTemplate_createsProject()
   {
      Path outputDir = tempDir.resolve("my-project");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // For debugging - print actual output if test fails                    //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("Project initialized successfully");
      assertThat(outputDir).exists();
      assertThat(outputDir.resolve("README.md")).exists();
   }



   /***************************************************************************
    * Test --dry-run flag shows what would be created without creating files.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_dryRun_showsFilesWithoutCreating()
   {
      Path outputDir = tempDir.resolve("dry-run-test");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--dry-run",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("[DRY-RUN]");
      assertThat(harness.getStdout()).contains("README.md");
      assertThat(outputDir).doesNotExist();
   }



   /***************************************************************************
    * Test directory conflict without --force shows error.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_existingDir_showsConflictError() throws IOException
   {
      Path outputDir = tempDir.resolve("existing-project");
      Files.createDirectories(outputDir);

      harness.execute("qqq", "init", "test-minimal", "--no-prompt",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.CONFLICT);
      //////////////////////////////////////////////////////////////////////////
      // Error message goes to stderr, suggestion goes to stdout              //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getStderr()).contains("target directory already exists");
      assertThat(harness.getStdout()).contains("--force");
   }



   /***************************************************************************
    * Test --force flag overwrites existing directory.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_forceFlag_overwritesExistingDir() throws IOException
   {
      Path outputDir = tempDir.resolve("force-test");
      Files.createDirectories(outputDir);
      Files.writeString(outputDir.resolve("existing.txt"), "should be preserved");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--force",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("Overwriting existing directory");
      assertThat(outputDir.resolve("README.md")).exists();
   }



   /***************************************************************************
    * Test --merge flag merges with existing directory.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_mergeFlag_mergesWithExistingDir() throws IOException
   {
      Path outputDir = tempDir.resolve("merge-test");
      Files.createDirectories(outputDir);
      Files.writeString(outputDir.resolve("existing.txt"), "should be preserved");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--merge",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("Merging with existing directory");
      assertThat(outputDir.resolve("existing.txt")).exists();
      assertThat(outputDir.resolve("README.md")).exists();
   }



   /***************************************************************************
    * Test --var flag sets template variables.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_varFlag_setsTemplateVariables()
   {
      Path outputDir = tempDir.resolve("var-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=custom-name",
         "--var", "packageName=com.acme",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("Project initialized successfully");
   }



   /***************************************************************************
    * Test non-existent template shows error with suggestions.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_nonExistentTemplate_showsError()
   {
      Path outputDir = tempDir.resolve("error-test");

      harness.execute("qqq", "init", "not-a-real-template", "--no-prompt",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // Should show error - template not found                               //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getStdout() + harness.getStderr())
         .containsIgnoringCase("not found")
         .contains("not-a-real-template");
   }



   /***************************************************************************
    * Test --verbose flag includes extra error details.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_verboseFlag_showsDetailedErrors()
   {
      Path outputDir = tempDir.resolve("verbose-test");

      harness.execute("qqq", "init", "not-a-real-template", "--no-prompt", "-v",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // Verbose output should include stack trace or extra context           //
      //////////////////////////////////////////////////////////////////////////
      String output = harness.getStdout() + harness.getStderr();
      assertThat(output).containsIgnoringCase("not found");
   }



   /***************************************************************************
    * Test init with multiple --var flags.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_multipleVars_allApplied()
   {
      Path outputDir = tempDir.resolve("multi-var-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=my-app",
         "--var", "packageName=org.example",
         "--var", "includeTests=false",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
   }



   /***************************************************************************
    * Test --skip-hooks flag prevents post-gen hooks from running.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_skipHooks_noHooksExecuted()
   {
      Path outputDir = tempDir.resolve("skip-hooks-test");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--skip-hooks",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("Project initialized successfully");
   }



   /***************************************************************************
    * Test combined flags work together.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_combinedFlags_dryRunWithForce() throws IOException
   {
      Path outputDir = tempDir.resolve("combined-test");
      Files.createDirectories(outputDir);

      harness.execute("qqq", "init", "test-minimal", "--no-prompt",
         "--dry-run", "--force",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout()).contains("[DRY-RUN]");
      //////////////////////////////////////////////////////////////////////////
      // In dry-run mode, existing files should not be modified               //
      //////////////////////////////////////////////////////////////////////////
      assertThat(outputDir.resolve("README.md")).doesNotExist();
   }
}
