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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.qrun.qctl.shared.ExitCodes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 * End-to-end tests using the native qctl binary.
 *
 * These tests spawn the actual native binary and verify exit codes and output.
 * Requires the native binary to be built (skipped if not present).
 *
 * @since 0.2.0
 *******************************************************************************/
class NativeBinaryE2ETest
{
   private static final String BINARY_PROPERTY = "qctl.native.binary";
   private static final long TIMEOUT_SECONDS = 30;

   private static Path binaryPath;

   @TempDir
   Path tempDir;



   /***************************************************************************
    * Check if native binary is available.
    *
    * @since 0.2.0
    ***************************************************************************/
   @BeforeAll
   static void checkBinaryExists()
   {
      String binaryPathStr = System.getProperty(BINARY_PROPERTY);
      if(binaryPathStr != null)
      {
         binaryPath = Path.of(binaryPathStr);
      }
      else
      {
         //////////////////////////////////////////////////////////////////
         // Try default location                                        //
         //////////////////////////////////////////////////////////////////
         binaryPath = Path.of("../qctl-cli/target/qctl");
      }
   }



   @BeforeEach
   void assumeBinaryPresent()
   {
      assumeTrue(Files.exists(binaryPath) && Files.isExecutable(binaryPath),
         "Native binary not found at " + binaryPath + ". Run 'mvn package -Pnative' to build.");
   }



   /***************************************************************************
    * Test --version flag.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testVersion_showsVersionNumber() throws Exception
   {
      ProcessResult result = execute("--version");

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout).containsPattern("qctl.*\\d+\\.\\d+\\.\\d+");
   }



   /***************************************************************************
    * Test --help flag.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testHelp_showsUsage() throws Exception
   {
      ProcessResult result = execute("--help");

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout)
         .contains("qctl")
         .contains("qqq")
         .contains("qbit")
         .contains("qrun");
   }



   /***************************************************************************
    * Test qqq init --help.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testQqqInitHelp_showsUsage() throws Exception
   {
      ProcessResult result = execute("qqq", "init", "--help");

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout)
         .contains("Initialize a new project")
         .contains("--output")
         .contains("--force")
         .contains("--merge");
   }



   /***************************************************************************
    * Test qqq list command.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testQqqList_showsTemplates() throws Exception
   {
      ProcessResult result = execute("qqq", "list");

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout)
         .contains("Available templates")
         .contains("new-qqq-application");
   }



   /***************************************************************************
    * Test unknown command shows error.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testUnknownCommand_showsError() throws Exception
   {
      ProcessResult result = execute("not-a-command");

      assertThat(result.exitCode).isNotZero();
      assertThat(result.stderr).contains("not-a-command");
   }



   /***************************************************************************
    * Test init with non-existent template.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_nonExistentTemplate_showsError() throws Exception
   {
      ProcessResult result = execute("qqq", "init", "not-a-real-template", "--no-prompt",
         "-o", tempDir.resolve("test-project").toString());

      // Should show error with suggestions
      assertThat(result.stdout + result.stderr)
         .containsIgnoringCase("not found")
         .contains("not-a-real-template");
   }



   /***************************************************************************
    * Test init --dry-run shows preview without creating files.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_dryRun_showsPreview() throws Exception
   {
      Path outputDir = tempDir.resolve("dry-run-project");

      ProcessResult result = execute("qqq", "init", "new-qqq-application",
         "--no-prompt", "--dry-run",
         "-o", outputDir.toString());

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout).contains("[DRY-RUN]");
      assertThat(outputDir).doesNotExist();
   }



   /***************************************************************************
    * Test init succeeds with new-qqq-application template.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_defaultTemplate_createsProject() throws Exception
   {
      Path outputDir = tempDir.resolve("default-template-project");

      ProcessResult result = execute("qqq", "init", "new-qqq-application",
         "--no-prompt",
         "--var", "applicationName=test-app",
         "--var", "groupId=com.test",
         "--var", "artifactId=test-app",
         "-o", outputDir.toString());

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout).contains("Project initialized successfully");
      assertThat(outputDir).exists();
   }



   /***************************************************************************
    * Test init with --force overwrites existing directory.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_force_overwritesExisting() throws Exception
   {
      Path outputDir = tempDir.resolve("force-project");
      Files.createDirectories(outputDir);
      Files.writeString(outputDir.resolve("existing.txt"), "test");

      ProcessResult result = execute("qqq", "init", "new-qqq-application",
         "--no-prompt", "--force",
         "--var", "applicationName=test-app",
         "--var", "groupId=com.test",
         "--var", "artifactId=test-app",
         "-o", outputDir.toString());

      assertThat(result.exitCode).isEqualTo(ExitCodes.SUCCESS);
      assertThat(result.stdout).contains("Overwriting");
   }



   /***************************************************************************
    * Test init without --force fails on existing directory.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_existingDir_showsConflict() throws Exception
   {
      Path outputDir = tempDir.resolve("existing-project");
      Files.createDirectories(outputDir);

      ProcessResult result = execute("qqq", "init", "new-qqq-application",
         "--no-prompt",
         "-o", outputDir.toString());

      assertThat(result.exitCode).isEqualTo(ExitCodes.CONFLICT);
      assertThat(result.stderr).contains("target directory already exists");
   }



   /***************************************************************************
    * Execute the native binary with arguments.
    *
    * @param args command-line arguments
    * @return process result with stdout, stderr, and exit code
    * @throws IOException if execution fails
    * @throws InterruptedException if wait is interrupted
    * @since 0.2.0
    ***************************************************************************/
   private ProcessResult execute(String... args) throws IOException, InterruptedException
   {
      List<String> command = new ArrayList<>();
      command.add(binaryPath.toAbsolutePath().toString());
      command.addAll(List.of(args));

      ProcessBuilder pb = new ProcessBuilder(command);
      pb.environment().put("QCTL_REGISTRY_TYPE", "mock");
      pb.directory(tempDir.toFile());

      Process process = pb.start();

      StringBuilder stdout = new StringBuilder();
      StringBuilder stderr = new StringBuilder();

      try(BufferedReader outReader = new BufferedReader(
         new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
          BufferedReader errReader = new BufferedReader(
             new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)))
      {
         Thread outThread = new Thread(() ->
         {
            try
            {
               String line;
               while((line = outReader.readLine()) != null)
               {
                  stdout.append(line).append("\n");
               }
            }
            catch(IOException e)
            {
               // Ignore
            }
         });

         Thread errThread = new Thread(() ->
         {
            try
            {
               String line;
               while((line = errReader.readLine()) != null)
               {
                  stderr.append(line).append("\n");
               }
            }
            catch(IOException e)
            {
               // Ignore
            }
         });

         outThread.start();
         errThread.start();

         boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
         if(!finished)
         {
            process.destroyForcibly();
            throw new IOException("Process timed out after " + TIMEOUT_SECONDS + " seconds");
         }

         outThread.join(1000);
         errThread.join(1000);
      }

      return new ProcessResult(stdout.toString(), stderr.toString(), process.exitValue());
   }



   /***************************************************************************
    * Result of a process execution.
    *
    * @since 0.2.0
    ***************************************************************************/
   record ProcessResult(String stdout, String stderr, int exitCode)
   {
   }
}
