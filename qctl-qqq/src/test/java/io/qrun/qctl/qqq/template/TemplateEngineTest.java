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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 * Unit tests for TemplateEngine with Velocity.
 *
 * @since 0.2.0
 *******************************************************************************/
class TemplateEngineTest
{
   @TempDir
   Path tempDir;

   private TemplateEngine engine;



   @BeforeEach
   void setUp()
   {
      engine = new TemplateEngine();
   }



   /***************************************************************************
    * Test simple variable substitution in content.
    ***************************************************************************/
   @Test
   void testRenderContentSimpleVariable() throws TemplateRenderException
   {
      String template = "Hello, $name!";
      Map<String, String> variables = Map.of("name", "World");

      String result = engine.renderContent(template, variables);

      assertThat(result).isEqualTo("Hello, World!");
   }



   /***************************************************************************
    * Test variable substitution with braces syntax.
    ***************************************************************************/
   @Test
   void testRenderContentBracesSyntax() throws TemplateRenderException
   {
      String template = "Package: ${packageName}";
      Map<String, String> variables = Map.of("packageName", "com.example.app");

      String result = engine.renderContent(template, variables);

      assertThat(result).isEqualTo("Package: com.example.app");
   }



   /***************************************************************************
    * Test StringTool usage in templates.
    ***************************************************************************/
   @Test
   void testRenderContentWithStringTool() throws TemplateRenderException
   {
      String template = "class $str.pascal($appName) {}";
      Map<String, String> variables = Map.of("appName", "myApplication");

      String result = engine.renderContent(template, variables);

      assertThat(result).isEqualTo("class MyApplication {}");
   }



   /***************************************************************************
    * Test all StringTool transformations in content.
    ***************************************************************************/
   @Test
   void testRenderContentAllStringTools() throws TemplateRenderException
   {
      String template = """
         lower: $str.lower($name)
         upper: $str.upper($name)
         camel: $str.camel($name)
         pascal: $str.pascal($name)
         kebab: $str.kebab($name)
         snake: $str.snake($name)""";

      Map<String, String> variables = Map.of("name", "MyProject");

      String result = engine.renderContent(template, variables);

      assertThat(result).isEqualTo("""
         lower: myproject
         upper: MYPROJECT
         camel: myProject
         pascal: MyProject
         kebab: my-project
         snake: my_project""");
   }



   /***************************************************************************
    * Test Velocity control flow - #if.
    ***************************************************************************/
   @Test
   void testRenderContentIfStatement() throws TemplateRenderException
   {
      String template = "#if($includeTests)tests included#end";
      Map<String, String> variables = Map.of("includeTests", "true");

      String result = engine.renderContent(template, variables);

      assertThat(result).isEqualTo("tests included");
   }



   /***************************************************************************
    * Test Velocity control flow - #foreach.
    ***************************************************************************/
   @Test
   void testRenderContentForeach() throws TemplateRenderException
   {
      String template = "#foreach($item in $items)$item #end";
      Map<String, Object> variables = new HashMap<>();
      variables.put("items", List.of("a", "b", "c"));

      String result = engine.renderContentWithObjects(template, variables);

      assertThat(result).isEqualTo("a b c ");
   }



   /***************************************************************************
    * Test path rendering with Velocity variables.
    ***************************************************************************/
   @Test
   void testRenderPath() throws TemplateRenderException
   {
      String path = "src/main/java/$packagePath/App.java";
      Map<String, String> variables = Map.of("packagePath", "com/example/app");

      String result = engine.renderPath(path, variables);

      assertThat(result).isEqualTo("src/main/java/com/example/app/App.java");
   }



   /***************************************************************************
    * Test path rendering with StringTool transformations.
    ***************************************************************************/
   @Test
   void testRenderPathWithStringTool() throws TemplateRenderException
   {
      String path = "src/$str.kebab($projectName)/main.java";
      Map<String, String> variables = Map.of("projectName", "MyProject");

      String result = engine.renderPath(path, variables);

      assertThat(result).isEqualTo("src/my-project/main.java");
   }



   /***************************************************************************
    * Test that missing required variable throws exception.
    ***************************************************************************/
   @Test
   void testRenderContentMissingVariableThrows()
   {
      String template = "Hello, $name!";
      Map<String, String> variables = Map.of(); // no variables

      assertThatThrownBy(() -> engine.renderContent(template, variables))
         .isInstanceOf(TemplateRenderException.class)
         .hasMessageContaining("name");
   }



   /***************************************************************************
    * Test that syntax error throws exception.
    ***************************************************************************/
   @Test
   void testRenderContentSyntaxErrorThrows()
   {
      String template = "#if($x) unclosed if";
      Map<String, String> variables = Map.of("x", "true");

      assertThatThrownBy(() -> engine.renderContent(template, variables))
         .isInstanceOf(TemplateRenderException.class);
   }



   /***************************************************************************
    * Test rendering full template directory.
    ***************************************************************************/
   @Test
   void testRenderDirectory() throws IOException, TemplateRenderException
   {
      // Set up source template
      Path templateDir = tempDir.resolve("template-source");
      Path templateSubdir = templateDir.resolve("template");
      Files.createDirectories(templateSubdir.resolve("src/main/java"));

      Files.writeString(templateSubdir.resolve("pom.xml"),
         "<artifactId>$artifactId</artifactId>");
      Files.writeString(templateSubdir.resolve("src/main/java/App.java"),
         "package $packageName;\npublic class $str.pascal($appName) {}");

      // Create manifest
      TemplateManifest manifest = new TemplateManifest(
         null, "test-template", "Test Template", null, null, null, null, null, null, null, null);

      // Render to target
      Path targetDir = tempDir.resolve("output");
      Files.createDirectories(targetDir);

      Map<String, String> variables = Map.of(
         "artifactId", "my-app",
         "packageName", "com.example",
         "appName", "myApp"
      );

      int fileCount = engine.render(templateDir, targetDir, variables, manifest);

      // Verify
      assertThat(fileCount).isEqualTo(2);
      assertThat(targetDir.resolve("pom.xml")).content()
         .isEqualTo("<artifactId>my-app</artifactId>");
      assertThat(targetDir.resolve("src/main/java/App.java")).content()
         .isEqualTo("package com.example;\npublic class MyApp {}");
   }



   /***************************************************************************
    * Test rendering with path variable substitution.
    ***************************************************************************/
   @Test
   void testRenderDirectoryWithPathVariables() throws IOException, TemplateRenderException
   {
      // Set up source template with variable in path
      Path templateDir = tempDir.resolve("template-source");
      Path templateSubdir = templateDir.resolve("template");
      Path srcPath = templateSubdir.resolve("src/main/java/$packagePath");
      Files.createDirectories(srcPath);

      Files.writeString(srcPath.resolve("App.java"),
         "package $packageName;\npublic class App {}");

      // Create manifest
      TemplateManifest manifest = new TemplateManifest(
         null, "test-template", "Test Template", null, null, null, null, null, null, null, null);

      // Render to target
      Path targetDir = tempDir.resolve("output");
      Files.createDirectories(targetDir);

      Map<String, String> variables = Map.of(
         "packageName", "com.acme.orders",
         "packagePath", "com/acme/orders"
      );

      int fileCount = engine.render(templateDir, targetDir, variables, manifest);

      // Verify path was rendered correctly
      assertThat(fileCount).isEqualTo(1);
      assertThat(targetDir.resolve("src/main/java/com/acme/orders/App.java"))
         .exists()
         .content().contains("package com.acme.orders;");
   }



   /***************************************************************************
    * Test binary files are copied without modification.
    ***************************************************************************/
   @Test
   void testRenderBinaryFileCopiedUnmodified() throws IOException, TemplateRenderException
   {
      // Set up source template with binary file
      Path templateDir = tempDir.resolve("template-source");
      Path templateSubdir = templateDir.resolve("template");
      Files.createDirectories(templateSubdir);

      byte[] binaryContent = {0x00, 0x01, 0x02, (byte) 0xFF};
      Files.write(templateSubdir.resolve("image.png"), binaryContent);

      // Create manifest
      TemplateManifest manifest = new TemplateManifest(
         null, "test-template", "Test Template", null, null, null, null, null, null, null, null);

      // Render to target
      Path targetDir = tempDir.resolve("output");
      Files.createDirectories(targetDir);

      int fileCount = engine.render(templateDir, targetDir, Map.of(), manifest);

      // Verify binary file copied as-is
      assertThat(fileCount).isEqualTo(1);
      assertThat(Files.readAllBytes(targetDir.resolve("image.png")))
         .isEqualTo(binaryContent);
   }



   /***************************************************************************
    * Test ignored files are skipped.
    ***************************************************************************/
   @Test
   void testRenderIgnoresFiles() throws IOException, TemplateRenderException
   {
      // Set up source template
      Path templateDir = tempDir.resolve("template-source");
      Path templateSubdir = templateDir.resolve("template");
      Files.createDirectories(templateSubdir);

      Files.writeString(templateSubdir.resolve("keep.txt"), "keep me");
      Files.writeString(templateSubdir.resolve("ignore.bak"), "ignore me");

      // Create manifest with ignore pattern
      TemplateManifest manifest = new TemplateManifest(
         null, "test-template", "Test Template", null, null, null, null, null, null, null, List.of("*.bak"));

      // Render to target
      Path targetDir = tempDir.resolve("output");
      Files.createDirectories(targetDir);

      int fileCount = engine.render(templateDir, targetDir, Map.of(), manifest);

      // Verify ignored file was skipped
      assertThat(fileCount).isEqualTo(1);
      assertThat(targetDir.resolve("keep.txt")).exists();
      assertThat(targetDir.resolve("ignore.bak")).doesNotExist();
   }



   /***************************************************************************
    * Test dry run mode shows files without creating them.
    ***************************************************************************/
   @Test
   void testRenderDryRun() throws IOException, TemplateRenderException
   {
      // Set up source template
      Path templateDir = tempDir.resolve("template-source");
      Path templateSubdir = templateDir.resolve("template");
      Files.createDirectories(templateSubdir);

      Files.writeString(templateSubdir.resolve("file.txt"), "content");

      // Create manifest
      TemplateManifest manifest = new TemplateManifest(
         null, "test-template", "Test Template", null, null, null, null, null, null, null, null);

      // Render dry run to target
      Path targetDir = tempDir.resolve("output");
      // Note: don't create targetDir - dry run should work without it

      engine.renderDryRun(templateDir, targetDir, Map.of(), manifest);

      // Verify no files were created
      assertThat(targetDir).doesNotExist();
   }
}
