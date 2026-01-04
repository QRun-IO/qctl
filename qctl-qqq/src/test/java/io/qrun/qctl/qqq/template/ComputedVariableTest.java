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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 * Unit tests for computed variable evaluation.
 *
 * @since 0.2.0
 *******************************************************************************/
class ComputedVariableTest
{
   private TemplateEngine engine;



   @BeforeEach
   void setUp()
   {
      engine = new TemplateEngine();
   }



   /***************************************************************************
    * Test basic computed variable using StringTool.replace().
    ***************************************************************************/
   @Test
   void testComputedPackagePath() throws TemplateRenderException
   {
      Map<String, String> variables = new HashMap<>();
      variables.put("packageName", "com.example.myapp");

      List<ComputedVariable> computed = List.of(
         new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')")
      );

      Map<String, String> result = engine.evaluateComputed(computed, variables);

      assertThat(result).containsEntry("packageName", "com.example.myapp");
      assertThat(result).containsEntry("packagePath", "com/example/myapp");
   }



   /***************************************************************************
    * Test computed variable using StringTool.pascal().
    ***************************************************************************/
   @Test
   void testComputedPascalCase() throws TemplateRenderException
   {
      Map<String, String> variables = new HashMap<>();
      variables.put("appName", "myApplication");

      List<ComputedVariable> computed = List.of(
         new ComputedVariable("appClassName", "$str.pascal($appName)")
      );

      Map<String, String> result = engine.evaluateComputed(computed, variables);

      assertThat(result).containsEntry("appClassName", "MyApplication");
   }



   /***************************************************************************
    * Test chained computed variables (one computed uses another).
    ***************************************************************************/
   @Test
   void testChainedComputedVariables() throws TemplateRenderException
   {
      Map<String, String> variables = new HashMap<>();
      variables.put("packageName", "com.example.app");

      List<ComputedVariable> computed = List.of(
         new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')"),
         new ComputedVariable("srcPath", "src/main/java/$packagePath")
      );

      Map<String, String> result = engine.evaluateComputed(computed, variables);

      assertThat(result).containsEntry("packagePath", "com/example/app");
      assertThat(result).containsEntry("srcPath", "src/main/java/com/example/app");
   }



   /***************************************************************************
    * Test computed variable referencing undefined variable throws exception.
    ***************************************************************************/
   @Test
   void testUndefinedVariableThrows()
   {
      Map<String, String> variables = new HashMap<>();
      // packageName is not defined

      /////////////////////////////////////////////////////////////////////////
      // Use simple reference, not method call (method calls may return "")  //
      /////////////////////////////////////////////////////////////////////////
      List<ComputedVariable> computed = List.of(
         new ComputedVariable("packagePath", "path: $packageName")
      );

      assertThatThrownBy(() -> engine.evaluateComputed(computed, variables))
         .isInstanceOf(TemplateRenderException.class)
         .hasMessageContaining("packageName");
   }



   /***************************************************************************
    * Test circular dependency detected (implicit via order).
    ***************************************************************************/
   @Test
   void testCircularDependencyDetected()
   {
      Map<String, String> variables = new HashMap<>();

      /////////////////////////////////////////////////////////////////////////
      // varA references varB, but varB is defined after varA               //
      /////////////////////////////////////////////////////////////////////////
      List<ComputedVariable> computed = List.of(
         new ComputedVariable("varA", "$varB"),
         new ComputedVariable("varB", "value")
      );

      assertThatThrownBy(() -> engine.evaluateComputed(computed, variables))
         .isInstanceOf(TemplateRenderException.class)
         .hasMessageContaining("varB");
   }



   /***************************************************************************
    * Test empty computed list returns original variables.
    ***************************************************************************/
   @Test
   void testEmptyComputedReturnsOriginalVariables() throws TemplateRenderException
   {
      Map<String, String> variables = Map.of("name", "test");

      Map<String, String> result = engine.evaluateComputed(List.of(), variables);

      assertThat(result).isEqualTo(variables);
   }



   /***************************************************************************
    * Test null computed list returns original variables.
    ***************************************************************************/
   @Test
   void testNullComputedReturnsOriginalVariables() throws TemplateRenderException
   {
      Map<String, String> variables = Map.of("name", "test");

      Map<String, String> result = engine.evaluateComputed(null, variables);

      assertThat(result).isEqualTo(variables);
   }



   /***************************************************************************
    * Test multiple StringTool transformations in one expression.
    ***************************************************************************/
   @Test
   void testChainedTransformations() throws TemplateRenderException
   {
      Map<String, String> variables = new HashMap<>();
      variables.put("appName", "myApp");

      List<ComputedVariable> computed = List.of(
         new ComputedVariable("kebabName", "$str.kebab($appName)"),
         new ComputedVariable("upperKebab", "$str.upper($kebabName)")
      );

      Map<String, String> result = engine.evaluateComputed(computed, variables);

      assertThat(result).containsEntry("kebabName", "my-app");
      assertThat(result).containsEntry("upperKebab", "MY-APP");
   }



   /***************************************************************************
    * Test computed variables work with full template rendering.
    ***************************************************************************/
   @Test
   void testComputedVariablesInTemplateRendering() throws TemplateRenderException
   {
      Map<String, String> variables = new HashMap<>();
      variables.put("packageName", "com.example.myapp");
      variables.put("appName", "myApp");

      List<ComputedVariable> computed = List.of(
         new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')"),
         new ComputedVariable("appClassName", "$str.pascal($appName)")
      );

      Map<String, String> allVariables = engine.evaluateComputed(computed, variables);

      String template = "package $packageName;\n\npublic class $appClassName {}";
      String result = engine.renderContent(template, allVariables);

      assertThat(result).isEqualTo("package com.example.myapp;\n\npublic class MyApp {}");
   }
}
