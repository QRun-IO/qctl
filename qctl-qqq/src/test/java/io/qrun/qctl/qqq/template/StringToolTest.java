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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for StringTool.
 *
 * @since 0.2.0
 *******************************************************************************/
class StringToolTest
{
   private StringTool stringTool;



   @BeforeEach
   void setUp()
   {
      stringTool = new StringTool();
   }



   /***************************************************************************
    * Test lower() with various inputs.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "HelloWorld, helloworld",
      "UPPERCASE, uppercase",
      "already-lower, already-lower",
      "MixedCase123, mixedcase123"
   })
   void testLower(String input, String expected)
   {
      assertThat(stringTool.lower(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test lower() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testLowerNullAndEmpty(String input)
   {
      assertThat(stringTool.lower(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test upper() with various inputs.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "HelloWorld, HELLOWORLD",
      "lowercase, LOWERCASE",
      "already-UPPER, ALREADY-UPPER",
      "MixedCase123, MIXEDCASE123"
   })
   void testUpper(String input, String expected)
   {
      assertThat(stringTool.upper(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test upper() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testUpperNullAndEmpty(String input)
   {
      assertThat(stringTool.upper(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test camel() converts to camelCase.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "HelloWorld, helloWorld",
      "PascalCase, pascalCase",
      "already-camel, already-camel",
      "X, x"
   })
   void testCamel(String input, String expected)
   {
      assertThat(stringTool.camel(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test camel() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testCamelNullAndEmpty(String input)
   {
      assertThat(stringTool.camel(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test pascal() converts to PascalCase.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "helloWorld, HelloWorld",
      "camelCase, CamelCase",
      "x, X"
   })
   void testPascal(String input, String expected)
   {
      assertThat(stringTool.pascal(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test pascal() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testPascalNullAndEmpty(String input)
   {
      assertThat(stringTool.pascal(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test kebab() converts camelCase to kebab-case.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "helloWorld, hello-world",
      "HelloWorld, hello-world",
      "myApiName, my-api-name",
      "simple, simple"
   })
   void testKebab(String input, String expected)
   {
      assertThat(stringTool.kebab(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test kebab() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testKebabNullAndEmpty(String input)
   {
      assertThat(stringTool.kebab(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test snake() converts camelCase to snake_case.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "helloWorld, hello_world",
      "HelloWorld, hello_world",
      "myApiName, my_api_name",
      "simple, simple"
   })
   void testSnake(String input, String expected)
   {
      assertThat(stringTool.snake(input)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test snake() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testSnakeNullAndEmpty(String input)
   {
      assertThat(stringTool.snake(input)).isEqualTo("");
   }



   /***************************************************************************
    * Test that StringTool can be used in a Velocity-like context.
    ***************************************************************************/
   @Test
   void testChainedTransformations()
   {
      // Simulating $str.upper($str.kebab($name))
      String name = "myProjectName";
      String kebab = stringTool.kebab(name);
      String upper = stringTool.upper(kebab);

      assertThat(upper).isEqualTo("MY-PROJECT-NAME");
   }



   /***************************************************************************
    * Test replace() for package path derivation.
    ***************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "com.example.app, ., /, com/example/app",
      "hello-world, -, _, hello_world",
      "no_match, x, y, no_match",
      "aaa, a, b, bbb"
   })
   void testReplace(String input, String target, String replacement, String expected)
   {
      assertThat(stringTool.replace(input, target, replacement)).isEqualTo(expected);
   }



   /***************************************************************************
    * Test replace() with null and empty values.
    ***************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   void testReplaceNullAndEmpty(String input)
   {
      assertThat(stringTool.replace(input, ".", "/")).isEqualTo("");
   }



   /***************************************************************************
    * Test replace() with null target or replacement.
    ***************************************************************************/
   @Test
   void testReplaceNullTargetOrReplacement()
   {
      assertThat(stringTool.replace("test", null, "/")).isEqualTo("test");
      assertThat(stringTool.replace("test", ".", null)).isEqualTo("test");
   }
}
