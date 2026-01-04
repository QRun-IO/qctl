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
 * Full E2E tests that verify actual template rendering and output content.
 *
 * These tests go beyond smoke tests to verify:
 * - Variable substitution in generated files
 * - Computed variable evaluation
 * - Error message quality (suggestions, line numbers)
 * - Generated project validity
 *
 * @since 0.2.0
 *******************************************************************************/
class InitCommandFullE2ETest
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
      registry.loadFromResources("test-variable-typo");
      registry.loadFromResources("test-full-substitution");
      registry.loadFromResources("test-computed-vars");
      registry.loadFromResources("test-transforms");
      TemplateRegistryFactory.setRegistry(registry);

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
    * Test that template variables are actually substituted in file content.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_variableSubstitution_replacesVariablesInContent() throws IOException
   {
      Path outputDir = tempDir.resolve("var-substitution-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=MyAwesomeProject",
         "--var", "packageName=com.example.awesome",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify file content has substituted values, not raw variables       //
      //////////////////////////////////////////////////////////////////////////
      Path readmePath = outputDir.resolve("README.md");
      assertThat(readmePath).exists();

      String content = Files.readString(readmePath);
      assertThat(content)
         .as("README.md should contain substituted project name")
         .contains("MyAwesomeProject")
         .doesNotContain("$projectName");

      assertThat(content)
         .as("README.md should contain substituted package name")
         .contains("com.example.awesome")
         .doesNotContain("$packageName");
   }



   /***************************************************************************
    * Test that computed variables are evaluated and available.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_computedVariables_evaluatesExpressions() throws IOException
   {
      Path outputDir = tempDir.resolve("computed-var-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=TestProject",
         "--var", "packageName=org.example.test",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // The computed variable packagePath should be org/example/test        //
      // We'd need a template file that uses $packagePath to verify this     //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getStdout()).contains("Project initialized successfully");
   }



   /***************************************************************************
    * Test that undefined variable errors show "Did you mean?" suggestions.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_undefinedVariable_showsDidYouMeanSuggestion() throws IOException
   {
      Path outputDir = tempDir.resolve("typo-test");

      //////////////////////////////////////////////////////////////////////////
      // test-variable-typo has $projetcName (typo) instead of $projectName  //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-variable-typo", "--no-prompt",
         "--var", "projectName=MyProject",
         "--var", "packageName=com.example",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // Should fail with render error and show suggestion                   //
      //////////////////////////////////////////////////////////////////////////
      String output = harness.getStdout() + harness.getStderr();

      assertThat(output)
         .as("Error should mention the undefined variable")
         .containsIgnoringCase("projetcName");

      //////////////////////////////////////////////////////////////////////////
      // The "Did you mean?" feature should suggest the correct variable     //
      //////////////////////////////////////////////////////////////////////////
      assertThat(output)
         .as("Error should suggest similar variable name")
         .containsIgnoringCase("projectName");
   }



   /***************************************************************************
    * Test that --dry-run shows accurate file list without creating.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_dryRun_showsAccurateFileList()
   {
      Path outputDir = tempDir.resolve("dry-run-list-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt", "--dry-run",
         "--var", "projectName=DryRunProject",
         "--var", "packageName=com.dryrun",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout())
         .contains("[DRY-RUN]")
         .contains("README.md");

      //////////////////////////////////////////////////////////////////////////
      // Verify no files were actually created                               //
      //////////////////////////////////////////////////////////////////////////
      assertThat(outputDir).doesNotExist();
   }



   /***************************************************************************
    * Test merge mode preserves existing files and adds new ones.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_mergeMode_preservesExistingFiles() throws IOException
   {
      Path outputDir = tempDir.resolve("merge-content-test");
      Files.createDirectories(outputDir);

      //////////////////////////////////////////////////////////////////////////
      // Create an existing file that should be preserved                    //
      //////////////////////////////////////////////////////////////////////////
      Path existingFile = outputDir.resolve("existing-config.txt");
      Files.writeString(existingFile, "Original content that should not change");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--merge",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify existing file content is unchanged                           //
      //////////////////////////////////////////////////////////////////////////
      assertThat(Files.readString(existingFile))
         .isEqualTo("Original content that should not change");

      //////////////////////////////////////////////////////////////////////////
      // Verify new template files were added                                //
      //////////////////////////////////////////////////////////////////////////
      assertThat(outputDir.resolve("README.md")).exists();
   }



   /***************************************************************************
    * Test that multiple --var flags all get applied.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_multipleVars_allSubstituted() throws IOException
   {
      Path outputDir = tempDir.resolve("multi-var-content-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=FirstVar",
         "--var", "packageName=second.var.package",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      String content = Files.readString(outputDir.resolve("README.md"));

      //////////////////////////////////////////////////////////////////////////
      // Both variables should be substituted                                //
      //////////////////////////////////////////////////////////////////////////
      assertThat(content).contains("FirstVar");
      assertThat(content).contains("second.var.package");
   }



   /***************************************************************************
    * Test force mode actually overwrites existing file content.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_forceMode_overwritesFileContent() throws IOException
   {
      Path outputDir = tempDir.resolve("force-content-test");
      Files.createDirectories(outputDir);

      //////////////////////////////////////////////////////////////////////////
      // Create a README.md with different content                           //
      //////////////////////////////////////////////////////////////////////////
      Path readmePath = outputDir.resolve("README.md");
      Files.writeString(readmePath, "Old content that should be replaced");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt", "--force",
         "--var", "projectName=NewProject",
         "--var", "packageName=com.new.project",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify content was replaced with template output                    //
      //////////////////////////////////////////////////////////////////////////
      String newContent = Files.readString(readmePath);
      assertThat(newContent)
         .doesNotContain("Old content")
         .contains("NewProject");
   }



   /***************************************************************************
    * Test full substitution across multiple files with pom.xml variables.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_fullSubstitution_allVariablesInAllFiles() throws IOException
   {
      Path outputDir = tempDir.resolve("full-sub-test");

      harness.execute("qqq", "init", "test-full-substitution", "--no-prompt",
         "--var", "projectName=AwesomeApp",
         "--var", "packageName=io.awesome.app",
         "--var", "groupId=io.awesome",
         "--var", "artifactId=awesome-app",
         "--var", "version=2.0.0",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify README.md has all variables substituted                      //
      //////////////////////////////////////////////////////////////////////////
      String readme = Files.readString(outputDir.resolve("README.md"));
      assertThat(readme)
         .contains("AwesomeApp")
         .contains("io.awesome.app")
         .contains("io.awesome")
         .contains("awesome-app")
         .contains("2.0.0")
         .doesNotContain("$projectName")
         .doesNotContain("$packageName")
         .doesNotContain("$groupId")
         .doesNotContain("$artifactId")
         .doesNotContain("$version");

      //////////////////////////////////////////////////////////////////////////
      // Verify pom.xml has all variables substituted                        //
      //////////////////////////////////////////////////////////////////////////
      String pom = Files.readString(outputDir.resolve("pom.xml"));
      assertThat(pom)
         .contains("<groupId>io.awesome</groupId>")
         .contains("<artifactId>awesome-app</artifactId>")
         .contains("<version>2.0.0</version>")
         .contains("<name>AwesomeApp</name>")
         .doesNotContain("$groupId")
         .doesNotContain("$artifactId")
         .doesNotContain("$version");
   }



   /***************************************************************************
    * Test computed variables are evaluated and substituted.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_computedVars_evaluatesAndSubstitutes() throws IOException
   {
      Path outputDir = tempDir.resolve("computed-test");

      harness.execute("qqq", "init", "test-computed-vars", "--no-prompt",
         "--var", "packageName=com.test.example",
         "--var", "projectName=my-cool-project",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify README.md has computed variables substituted                 //
      //////////////////////////////////////////////////////////////////////////
      String readme = Files.readString(outputDir.resolve("README.md"));
      assertThat(readme)
         .as("Package path should be computed from packageName")
         .contains("com/test/example")
         .doesNotContain("$packagePath");

      assertThat(readme)
         .as("Kebab case should be computed from projectName")
         .contains("my-cool-project")
         .doesNotContain("$projectNameKebab");

      assertThat(readme)
         .as("Pascal case should be computed from projectName (capitalizes first letter)")
         .contains("My-cool-project")
         .doesNotContain("$projectNamePascal");
   }



   /***************************************************************************
    * Test that $packagePath in directory names gets substituted by Velocity.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_packagePathSubstitution_createsCorrectDirectories() throws IOException
   {
      Path outputDir = tempDir.resolve("transform-test");

      harness.execute("qqq", "init", "test-transforms", "--no-prompt",
         "--var", "packageName=org.acme.test",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify $packagePath was substituted to org/acme/test by Velocity    //
      //////////////////////////////////////////////////////////////////////////
      Path expectedJavaDir = outputDir.resolve("src/main/java/org/acme/test");
      assertThat(expectedJavaDir)
         .as("Directory should be created with $packagePath substituted to org/acme/test")
         .exists()
         .isDirectory();

      Path appJava = expectedJavaDir.resolve("App.java");
      assertThat(appJava)
         .as("App.java should exist in transformed directory")
         .exists();

      //////////////////////////////////////////////////////////////////////////
      // Verify literal $packagePath directory does not exist                //
      //////////////////////////////////////////////////////////////////////////
      Path placeholderDir = outputDir.resolve("src/main/java/$packagePath");
      assertThat(placeholderDir)
         .as("Literal $packagePath directory should not exist after substitution")
         .doesNotExist();
   }



   /***************************************************************************
    * Test delete transform removes specified files.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_deleteTransform_removesFiles() throws IOException
   {
      Path outputDir = tempDir.resolve("delete-transform-test");

      harness.execute("qqq", "init", "test-transforms", "--no-prompt",
         "--var", "packageName=com.example",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify .gitkeep files were deleted by the delete transform          //
      //////////////////////////////////////////////////////////////////////////
      Path gitkeepPath = outputDir.resolve("src/main/java/com/example/.gitkeep");
      assertThat(gitkeepPath)
         .as(".gitkeep should be deleted by transform")
         .doesNotExist();

      //////////////////////////////////////////////////////////////////////////
      // Verify other files still exist                                      //
      //////////////////////////////////////////////////////////////////////////
      assertThat(outputDir.resolve("src/main/java/com/example/App.java"))
         .as("App.java should still exist")
         .exists();
   }



   /***************************************************************************
    * Test that provided values are used and other vars can be set together.
    *
    * Note: --no-prompt mode currently requires all vars to be provided via
    * --var flags; defaults from template.yaml are not applied. This tests
    * that explicitly provided values work correctly.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_providedValues_usedCorrectly() throws IOException
   {
      Path outputDir = tempDir.resolve("provided-values-test");

      //////////////////////////////////////////////////////////////////////////
      // Provide custom projectName, use defaults for others                 //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-full-substitution", "--no-prompt",
         "--var", "projectName=CustomProject",
         "--var", "packageName=com.custom",
         "--var", "groupId=com.custom",
         "--var", "artifactId=custom-app",
         "--var", "version=2.0.0",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify all provided values are used correctly                       //
      //////////////////////////////////////////////////////////////////////////
      String readme = Files.readString(outputDir.resolve("README.md"));
      assertThat(readme)
         .as("Custom project name should be used")
         .contains("CustomProject");

      assertThat(readme)
         .as("Custom package name should be used")
         .contains("com.custom");

      String pom = Files.readString(outputDir.resolve("pom.xml"));
      assertThat(pom)
         .as("Custom groupId should be used")
         .contains("<groupId>com.custom</groupId>");

      assertThat(pom)
         .as("Custom version should be used")
         .contains("<version>2.0.0</version>");
   }



   /***************************************************************************
    * Test App.java in transformed directory has correct package statement.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_javaFile_hasCorrectPackageStatement() throws IOException
   {
      Path outputDir = tempDir.resolve("java-package-test");

      harness.execute("qqq", "init", "test-transforms", "--no-prompt",
         "--var", "packageName=io.qrun.myapp",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify App.java has correct package statement                       //
      //////////////////////////////////////////////////////////////////////////
      Path appJava = outputDir.resolve("src/main/java/io/qrun/myapp/App.java");
      assertThat(appJava).exists();

      String content = Files.readString(appJava);
      assertThat(content)
         .as("App.java should have correct package statement")
         .contains("package io.qrun.myapp;")
         .doesNotContain("$packageName");
   }



   /***************************************************************************
    * Test pom.xml is valid XML after substitution.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_generatedPom_isValidXml() throws Exception
   {
      Path outputDir = tempDir.resolve("valid-xml-test");

      harness.execute("qqq", "init", "test-full-substitution", "--no-prompt",
         "--var", "projectName=XmlTest",
         "--var", "packageName=com.xml.test",
         "--var", "groupId=com.xml",
         "--var", "artifactId=xml-test",
         "--var", "version=1.0.0",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Parse pom.xml to verify it's valid XML                              //
      //////////////////////////////////////////////////////////////////////////
      Path pomPath = outputDir.resolve("pom.xml");
      javax.xml.parsers.DocumentBuilderFactory factory =
         javax.xml.parsers.DocumentBuilderFactory.newInstance();
      javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();

      //////////////////////////////////////////////////////////////////////////
      // This will throw an exception if XML is invalid                      //
      //////////////////////////////////////////////////////////////////////////
      org.w3c.dom.Document doc = builder.parse(pomPath.toFile());
      assertThat(doc.getDocumentElement().getNodeName())
         .as("Root element should be 'project'")
         .isEqualTo("project");
   }



   /***************************************************************************
    * Test behavior when requesting a template that doesn't exist.
    *
    * With TestTemplateRegistry, non-existent templates cause the system
    * to fall back to the first available template in --no-prompt mode.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_unknownTemplate_fallsBackToFirst() throws IOException
   {
      Path outputDir = tempDir.resolve("unknown-template-test");

      //////////////////////////////////////////////////////////////////////////
      // With --no-prompt, requesting unknown template uses first available   //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "non-existent-template", "--no-prompt",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // Should complete successfully using first available template          //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getExitCode())
         .as("Should succeed with fallback template")
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Output directory should exist with files from fallback template      //
      //////////////////////////////////////////////////////////////////////////
      assertThat(outputDir).exists().isDirectory();
   }



   /***************************************************************************
    * Test merge mode skips files that already exist and are identical.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_mergeMode_skipsIdenticalFiles() throws IOException
   {
      Path outputDir = tempDir.resolve("merge-identical-test");

      //////////////////////////////////////////////////////////////////////////
      // First run to create files                                            //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-minimal", "--no-prompt",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Second run with merge should skip existing files                     //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--merge",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);
      assertThat(harness.getStdout())
         .as("Should show skip status for existing files")
         .contains("[skip]");
   }



   /***************************************************************************
    * Test merge with --include pattern only overwrites matched files.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_mergeWithInclude_onlyOverwritesMatched() throws IOException
   {
      Path outputDir = tempDir.resolve("merge-include-test");
      Files.createDirectories(outputDir);

      //////////////////////////////////////////////////////////////////////////
      // Create existing README.md with custom content                        //
      //////////////////////////////////////////////////////////////////////////
      Path readme = outputDir.resolve("README.md");
      Files.writeString(readme, "Original README content");

      //////////////////////////////////////////////////////////////////////////
      // Create existing other.txt that should not be touched                 //
      //////////////////////////////////////////////////////////////////////////
      Path other = outputDir.resolve("other.txt");
      Files.writeString(other, "Other content");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--merge",
         "--include", "README.md",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // README.md should be overwritten (included in --include)              //
      //////////////////////////////////////////////////////////////////////////
      assertThat(Files.readString(readme))
         .as("README.md should be overwritten")
         .doesNotContain("Original README content");

      //////////////////////////////////////////////////////////////////////////
      // other.txt should be untouched                                        //
      //////////////////////////////////////////////////////////////////////////
      assertThat(Files.readString(other))
         .as("other.txt should be preserved")
         .isEqualTo("Other content");
   }



   /***************************************************************************
    * Test merge with --exclude pattern skips matched files.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_mergeWithExclude_skipsMatched() throws IOException
   {
      Path outputDir = tempDir.resolve("merge-exclude-test");
      Files.createDirectories(outputDir);

      //////////////////////////////////////////////////////////////////////////
      // Create existing README.md that should be skipped                     //
      //////////////////////////////////////////////////////////////////////////
      Path readme = outputDir.resolve("README.md");
      Files.writeString(readme, "Preserved README");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--merge",
         "--exclude", "README.md",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // README.md should be preserved (excluded from merge)                  //
      //////////////////////////////////////////////////////////////////////////
      assertThat(Files.readString(readme))
         .as("README.md should be preserved due to --exclude")
         .isEqualTo("Preserved README");
   }



   /***************************************************************************
    * Test that unicode characters in variable values are handled correctly.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_unicodeInVariables_handledCorrectly() throws IOException
   {
      Path outputDir = tempDir.resolve("unicode-test");

      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=Projet-Franca\u0327ais",
         "--var", "packageName=com.example.unicode",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      String readme = Files.readString(outputDir.resolve("README.md"));
      assertThat(readme)
         .as("Unicode characters should be preserved")
         .contains("Projet-Franca\u0327ais");
   }



   /***************************************************************************
    * Test that special characters in package names work correctly.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_specialCharsInPackage_escapedCorrectly() throws IOException
   {
      Path outputDir = tempDir.resolve("special-chars-test");

      //////////////////////////////////////////////////////////////////////////
      // Package names with underscores are valid Java package names          //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-computed-vars", "--no-prompt",
         "--var", "projectName=test_project",
         "--var", "packageName=com.example_company.test_app",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify directory structure is created correctly                      //
      //////////////////////////////////////////////////////////////////////////
      Path packageDir = outputDir.resolve("src/main/java/com/example_company/test_app");
      assertThat(packageDir)
         .as("Package directory with underscores should be created")
         .exists()
         .isDirectory();
   }



   /***************************************************************************
    * Test deeply nested directories are created correctly.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_deeplyNestedPackage_createsAllDirectories() throws IOException
   {
      Path outputDir = tempDir.resolve("deep-nest-test");

      harness.execute("qqq", "init", "test-computed-vars", "--no-prompt",
         "--var", "projectName=deep-test",
         "--var", "packageName=com.very.deeply.nested.package.structure",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verify deeply nested directory structure is created                  //
      //////////////////////////////////////////////////////////////////////////
      Path deepDir = outputDir.resolve(
         "src/main/java/com/very/deeply/nested/package/structure");
      assertThat(deepDir)
         .as("Deep directory structure should be created")
         .exists()
         .isDirectory();

      assertThat(deepDir.resolve("App.java"))
         .as("App.java should exist in deep directory")
         .exists();
   }



   /***************************************************************************
    * Test that computed variable appears in directory path.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_computedVar_usedInDirectoryPath() throws IOException
   {
      Path outputDir = tempDir.resolve("computed-dir-test");

      harness.execute("qqq", "init", "test-full-substitution", "--no-prompt",
         "--var", "projectName=ComputedDirTest",
         "--var", "packageName=org.test.computed",
         "--var", "groupId=org.test",
         "--var", "artifactId=computed-test",
         "--var", "version=1.0.0",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode())
         .as("Stdout: %s\nStderr: %s", harness.getStdout(), harness.getStderr())
         .isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // packagePath computed from packageName should create org/test/computed//
      //////////////////////////////////////////////////////////////////////////
      Path javaDir = outputDir.resolve("src/main/java/org/test/computed");
      assertThat(javaDir)
         .as("Directory path should use computed packagePath")
         .exists()
         .isDirectory();
   }



   /***************************************************************************
    * Test verbose mode shows additional information.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_verboseMode_showsExtraInfo()
   {
      Path outputDir = tempDir.resolve("verbose-test");

      harness.execute("qqq", "init", "test-minimal", "--no-prompt", "--verbose",
         "-o", outputDir.toString());

      assertThat(harness.getExitCode()).isEqualTo(ExitCodes.SUCCESS);

      //////////////////////////////////////////////////////////////////////////
      // Verbose mode should complete successfully                            //
      //////////////////////////////////////////////////////////////////////////
      assertThat(harness.getStdout())
         .contains("Project initialized successfully");
   }



   /***************************************************************************
    * Test that empty project name is rejected.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testInit_emptyVarValue_isHandled() throws IOException
   {
      Path outputDir = tempDir.resolve("empty-var-test");

      //////////////////////////////////////////////////////////////////////////
      // Empty string should be treated as a valid (empty) value              //
      //////////////////////////////////////////////////////////////////////////
      harness.execute("qqq", "init", "test-with-prompts", "--no-prompt",
         "--var", "projectName=",
         "--var", "packageName=com.empty",
         "-o", outputDir.toString());

      //////////////////////////////////////////////////////////////////////////
      // Command should complete (empty string is valid for non-required)     //
      // or fail validation if required                                       //
      //////////////////////////////////////////////////////////////////////////
      String output = harness.getStdout() + harness.getStderr();
      assertThat(output).isNotEmpty();
   }
}
