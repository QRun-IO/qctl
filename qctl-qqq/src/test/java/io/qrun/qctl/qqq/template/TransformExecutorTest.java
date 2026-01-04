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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 * Unit tests for TransformExecutor.
 *
 * @since 0.2.0
 *******************************************************************************/
class TransformExecutorTest
{
   @TempDir
   Path tempDir;

   private TransformExecutor executor;



   @BeforeEach
   void setUp()
   {
      executor = new TransformExecutor();
   }



   /***************************************************************************
    * Test delete transform removes matching files.
    ***************************************************************************/
   @Test
   void testDeleteTransform() throws IOException, TemplateRenderException
   {
      // Arrange
      Path gitkeep1 = tempDir.resolve("src/.gitkeep");
      Path gitkeep2 = tempDir.resolve("test/.gitkeep");
      Path keepFile = tempDir.resolve("src/Main.java");

      Files.createDirectories(gitkeep1.getParent());
      Files.createDirectories(gitkeep2.getParent());
      Files.writeString(gitkeep1, "");
      Files.writeString(gitkeep2, "");
      Files.writeString(keepFile, "class Main {}");

      Transform deleteTransform = new Transform(
         Transform.TYPE_DELETE,
         "**/.gitkeep",
         null
      );

      // Act
      executor.execute(tempDir, List.of(deleteTransform), Map.of());

      // Assert
      assertThat(gitkeep1).doesNotExist();
      assertThat(gitkeep2).doesNotExist();
      assertThat(keepFile).exists();
   }



   /***************************************************************************
    * Test rename transform renames matching files.
    ***************************************************************************/
   @Test
   void testRenameTransform() throws IOException, TemplateRenderException
   {
      // Arrange
      Path templateFile = tempDir.resolve("src/TemplateApp.java");
      Files.createDirectories(templateFile.getParent());
      Files.writeString(templateFile, "class TemplateApp {}");

      Transform renameTransform = new Transform(
         Transform.TYPE_RENAME,
         "**/TemplateApp.java",
         "MyApp.java"
      );

      // Act
      executor.execute(tempDir, List.of(renameTransform), Map.of());

      // Assert
      assertThat(templateFile).doesNotExist();
      assertThat(tempDir.resolve("src/MyApp.java")).exists();
   }



   /***************************************************************************
    * Test rename transform with variable substitution.
    ***************************************************************************/
   @Test
   void testRenameTransformWithVariables() throws IOException, TemplateRenderException
   {
      // Arrange
      Path templateFile = tempDir.resolve("src/TemplateApplication.java");
      Files.createDirectories(templateFile.getParent());
      Files.writeString(templateFile, "class TemplateApplication {}");

      Transform renameTransform = new Transform(
         Transform.TYPE_RENAME,
         "**/TemplateApplication.java",
         "${appClassName}Application.java"
      );

      Map<String, String> variables = Map.of("appClassName", "MyApp");

      // Act
      executor.execute(tempDir, List.of(renameTransform), variables);

      // Assert
      assertThat(templateFile).doesNotExist();
      assertThat(tempDir.resolve("src/MyAppApplication.java")).exists();
   }



   /***************************************************************************
    * Test multiple transforms execute in order.
    ***************************************************************************/
   @Test
   void testMultipleTransforms() throws IOException, TemplateRenderException
   {
      // Arrange
      Path srcDir = tempDir.resolve("src");
      Files.createDirectories(srcDir);
      Path gitkeep = srcDir.resolve(".gitkeep");
      Path templateFile = srcDir.resolve("Template.java");
      Files.writeString(gitkeep, "");
      Files.writeString(templateFile, "class Template {}");

      List<Transform> transforms = List.of(
         new Transform(Transform.TYPE_DELETE, "**/.gitkeep", null),
         new Transform(Transform.TYPE_RENAME, "**/Template.java", "App.java")
      );

      // Act
      executor.execute(tempDir, transforms, Map.of());

      // Assert
      assertThat(gitkeep).doesNotExist();
      assertThat(templateFile).doesNotExist();
      assertThat(srcDir.resolve("App.java")).exists();
   }



   /***************************************************************************
    * Test empty transforms list does nothing.
    ***************************************************************************/
   @Test
   void testEmptyTransformsList() throws IOException, TemplateRenderException
   {
      // Arrange
      Path file = tempDir.resolve("test.txt");
      Files.writeString(file, "content");

      // Act
      executor.execute(tempDir, List.of(), Map.of());

      // Assert
      assertThat(file).exists();
   }



   /***************************************************************************
    * Test null transforms list does nothing.
    ***************************************************************************/
   @Test
   void testNullTransformsList() throws IOException, TemplateRenderException
   {
      // Arrange
      Path file = tempDir.resolve("test.txt");
      Files.writeString(file, "content");

      // Act
      executor.execute(tempDir, null, Map.of());

      // Assert
      assertThat(file).exists();
   }



   /***************************************************************************
    * Test rename preserves file content.
    ***************************************************************************/
   @Test
   void testRenamePreservesContent() throws IOException, TemplateRenderException
   {
      // Arrange
      String content = "package com.example;\n\nclass MyClass { }";
      Path srcDir = tempDir.resolve("src");
      Files.createDirectories(srcDir);
      Path originalFile = srcDir.resolve("Original.java");
      Files.writeString(originalFile, content);

      Transform renameTransform = new Transform(
         Transform.TYPE_RENAME,
         "**/Original.java",
         "Renamed.java"
      );

      // Act
      executor.execute(tempDir, List.of(renameTransform), Map.of());

      // Assert
      Path renamedFile = srcDir.resolve("Renamed.java");
      assertThat(renamedFile).exists();
      assertThat(Files.readString(renamedFile)).isEqualTo(content);
   }



   /***************************************************************************
    * Test rename in nested directory.
    ***************************************************************************/
   @Test
   void testRenameInNestedDirectory() throws IOException, TemplateRenderException
   {
      // Arrange
      Path nestedFile = tempDir.resolve("src/main/java/Template.java");
      Files.createDirectories(nestedFile.getParent());
      Files.writeString(nestedFile, "class Template {}");

      Transform renameTransform = new Transform(
         Transform.TYPE_RENAME,
         "**/Template.java",
         "App.java"
      );

      // Act
      executor.execute(tempDir, List.of(renameTransform), Map.of());

      // Assert
      assertThat(nestedFile).doesNotExist();
      assertThat(tempDir.resolve("src/main/java/App.java")).exists();
   }



   /***************************************************************************
    * Test pattern that matches nothing does not fail.
    ***************************************************************************/
   @Test
   void testNoMatchingFiles() throws IOException, TemplateRenderException
   {
      // Arrange
      Path file = tempDir.resolve("test.txt");
      Files.writeString(file, "content");

      Transform deleteTransform = new Transform(
         Transform.TYPE_DELETE,
         "**/*.nonexistent",
         null
      );

      // Act - should not throw
      executor.execute(tempDir, List.of(deleteTransform), Map.of());

      // Assert
      assertThat(file).exists();
   }



   /***************************************************************************
    * Test invalid transform type throws exception.
    ***************************************************************************/
   @Test
   void testInvalidTransformType()
   {
      Transform invalidTransform = new Transform("invalid", "**/*", null);

      assertThatThrownBy(() -> executor.execute(tempDir, List.of(invalidTransform), Map.of()))
         .isInstanceOf(TemplateRenderException.class)
         .hasMessageContaining("Invalid transform type");
   }



   /***************************************************************************
    * Test Transform validation - missing type.
    ***************************************************************************/
   @Test
   void testTransformValidationMissingType()
   {
      Transform transform = new Transform(null, "**/*", null);

      assertThatThrownBy(transform::validate)
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("type is required");
   }



   /***************************************************************************
    * Test Transform validation - missing pattern.
    ***************************************************************************/
   @Test
   void testTransformValidationMissingPattern()
   {
      Transform transform = new Transform(Transform.TYPE_DELETE, null, null);

      assertThatThrownBy(transform::validate)
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("pattern is required");
   }



   /***************************************************************************
    * Test Transform validation - rename without replacement.
    ***************************************************************************/
   @Test
   void testTransformValidationRenameWithoutReplacement()
   {
      Transform transform = new Transform(Transform.TYPE_RENAME, "**/*", null);

      assertThatThrownBy(transform::validate)
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Replacement is required");
   }
}
