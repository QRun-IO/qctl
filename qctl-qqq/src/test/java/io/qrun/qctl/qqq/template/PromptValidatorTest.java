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


import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for PromptValidator.
 *
 * @since 0.2.0
 *******************************************************************************/
class PromptValidatorTest
{
   private PromptValidator validator;



   @BeforeEach
   void setUp()
   {
      validator = new PromptValidator();
   }



   /***************************************************************************
    * Test required field with blank value fails.
    ***************************************************************************/
   @Test
   void testRequiredFieldBlankFails()
   {
      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, null, true);

      PromptValidator.ValidationResult result = validator.validate(prompt, "");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).isEqualTo("This field is required");
   }



   /***************************************************************************
    * Test required field with null value fails.
    ***************************************************************************/
   @Test
   void testRequiredFieldNullFails()
   {
      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, null, true);

      PromptValidator.ValidationResult result = validator.validate(prompt, null);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).isEqualTo("This field is required");
   }



   /***************************************************************************
    * Test required field with valid value passes.
    ***************************************************************************/
   @Test
   void testRequiredFieldWithValuePasses()
   {
      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, null, true);

      PromptValidator.ValidationResult result = validator.validate(prompt, "John");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test optional field with blank value passes.
    ***************************************************************************/
   @Test
   void testOptionalFieldBlankPasses()
   {
      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, null, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test pattern validation passes for valid input.
    ***************************************************************************/
   @Test
   void testPatternValidationPasses()
   {
      PromptValidation validation = new PromptValidation(
         "^[a-z][a-z0-9]*$", null, null, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "myapp123");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test pattern validation fails for invalid input.
    ***************************************************************************/
   @Test
   void testPatternValidationFails()
   {
      PromptValidation validation = new PromptValidation(
         "^[a-z][a-z0-9]*$", null, null, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "123invalid");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("pattern");
   }



   /***************************************************************************
    * Test custom error message is used on validation failure.
    ***************************************************************************/
   @Test
   void testCustomErrorMessage()
   {
      PromptValidation validation = new PromptValidation(
         "^[a-z]+$", null, null, null, null, null, "Must contain only lowercase letters");

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "ABC123");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).isEqualTo("Must contain only lowercase letters");
   }



   /***************************************************************************
    * Test minLength validation passes.
    ***************************************************************************/
   @Test
   void testMinLengthPasses()
   {
      PromptValidation validation = new PromptValidation(
         null, 3, null, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "abc");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test minLength validation fails.
    ***************************************************************************/
   @Test
   void testMinLengthFails()
   {
      PromptValidation validation = new PromptValidation(
         null, 5, null, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "ab");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("at least 5");
   }



   /***************************************************************************
    * Test maxLength validation passes.
    ***************************************************************************/
   @Test
   void testMaxLengthPasses()
   {
      PromptValidation validation = new PromptValidation(
         null, null, 10, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "short");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test maxLength validation fails.
    ***************************************************************************/
   @Test
   void testMaxLengthFails()
   {
      PromptValidation validation = new PromptValidation(
         null, null, 5, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "toolongvalue");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("at most 5");
   }



   /***************************************************************************
    * Test enum values validation passes.
    ***************************************************************************/
   @Test
   void testEnumValuesPasses()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, null, null, List.of("dev", "staging", "prod"), null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "env", "Environment", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "dev");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test enum values validation fails.
    ***************************************************************************/
   @Test
   void testEnumValuesFails()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, null, null, List.of("dev", "staging", "prod"), null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "env", "Environment", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "invalid");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("one of");
   }



   /***************************************************************************
    * Test integer validation passes.
    ***************************************************************************/
   @Test
   void testIntegerValidationPasses()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, 1, 100, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "count", "Count", "integer", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "50");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test integer min validation fails.
    ***************************************************************************/
   @Test
   void testIntegerMinFails()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, 10, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "count", "Count", "integer", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "5");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("at least 10");
   }



   /***************************************************************************
    * Test integer max validation fails.
    ***************************************************************************/
   @Test
   void testIntegerMaxFails()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, null, 100, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "count", "Count", "integer", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "200");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("at most 100");
   }



   /***************************************************************************
    * Test non-integer input fails for integer type.
    ***************************************************************************/
   @Test
   void testNonIntegerFails()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, 1, 100, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "count", "Count", "integer", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "abc");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("valid integer");
   }



   /***************************************************************************
    * Test invalid regex pattern in validation.
    ***************************************************************************/
   @Test
   void testInvalidPatternReportsError()
   {
      PromptValidation validation = new PromptValidation(
         "[invalid", null, null, null, null, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "test");

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("Invalid validation pattern");
   }



   /***************************************************************************
    * Test no validation rules passes.
    ***************************************************************************/
   @Test
   void testNoValidationRulesPasses()
   {
      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "name", "Name", "text", null, null, null, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "anything");

      assertThat(result.valid()).isTrue();
   }



   /***************************************************************************
    * Test number type works same as integer type.
    ***************************************************************************/
   @Test
   void testNumberTypeValidation()
   {
      PromptValidation validation = new PromptValidation(
         null, null, null, 1, 10, null, null);

      TemplateManifest.Prompt prompt = new TemplateManifest.Prompt(
         "count", "Count", "number", null, null, validation, false);

      PromptValidator.ValidationResult result = validator.validate(prompt, "5");

      assertThat(result.valid()).isTrue();
   }
}
