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


import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/*******************************************************************************
 * Validates prompt values against validation rules.
 *
 * Supports pattern matching, length constraints, numeric ranges, and enum values.
 *
 * @since 0.2.0
 *******************************************************************************/
public class PromptValidator
{
   /***************************************************************************
    * Validation result containing success status and error message.
    *
    * @param valid true if validation passed
    * @param errorMessage error message if validation failed
    * @since 0.2.0
    ***************************************************************************/
   public record ValidationResult(boolean valid, String errorMessage)
   {
      /*************************************************************************
       * Create a successful validation result.
       *
       * @return success result
       * @since 0.2.0
       *************************************************************************/
      public static ValidationResult success()
      {
         return new ValidationResult(true, null);
      }



      /*************************************************************************
       * Create a failed validation result.
       *
       * @param message the error message
       * @return failure result
       * @since 0.2.0
       *************************************************************************/
      public static ValidationResult failure(String message)
      {
         return new ValidationResult(false, message);
      }
   }



   /***************************************************************************
    * Validate a value against prompt rules.
    *
    * @param prompt the prompt definition with validation rules
    * @param value the value to validate
    * @return validation result
    * @since 0.2.0
    ***************************************************************************/
   public ValidationResult validate(TemplateManifest.Prompt prompt, String value)
   {
      // Check required
      if(prompt.isRequired() && (value == null || value.isBlank()))
      {
         return ValidationResult.failure("This field is required");
      }

      // If empty and not required, skip further validation
      if(value == null || value.isBlank())
      {
         return ValidationResult.success();
      }

      // Get validation rules
      PromptValidation validation = prompt.validation();
      if(validation == null)
      {
         return ValidationResult.success();
      }

      // Determine type
      String type = prompt.type() != null ? prompt.type().toLowerCase() : "text";

      // Type-specific validation
      return switch(type)
      {
         case "integer", "number" -> validateInteger(validation, value);
         default -> validateString(validation, value);
      };
   }



   /***************************************************************************
    * Validate a string value.
    *
    * @param validation validation rules
    * @param value the value to validate
    * @return validation result
    * @since 0.2.0
    ***************************************************************************/
   private ValidationResult validateString(PromptValidation validation, String value)
   {
      // Pattern validation
      if(validation.pattern() != null)
      {
         Optional<ValidationResult> patternResult = validatePattern(validation.pattern(), value, validation.message());
         if(patternResult.isPresent())
         {
            return patternResult.get();
         }
      }

      // Length validation
      if(validation.minLength() != null && value.length() < validation.minLength())
      {
         String message = validation.message() != null
            ? validation.message()
            : "Must be at least " + validation.minLength() + " characters";
         return ValidationResult.failure(message);
      }

      if(validation.maxLength() != null && value.length() > validation.maxLength())
      {
         String message = validation.message() != null
            ? validation.message()
            : "Must be at most " + validation.maxLength() + " characters";
         return ValidationResult.failure(message);
      }

      // Enum validation
      if(validation.values() != null && !validation.values().isEmpty())
      {
         if(!validation.values().contains(value))
         {
            String message = validation.message() != null
               ? validation.message()
               : "Must be one of: " + String.join(", ", validation.values());
            return ValidationResult.failure(message);
         }
      }

      return ValidationResult.success();
   }



   /***************************************************************************
    * Validate an integer value.
    *
    * @param validation validation rules
    * @param value the value to validate
    * @return validation result
    * @since 0.2.0
    ***************************************************************************/
   private ValidationResult validateInteger(PromptValidation validation, String value)
   {
      // Parse as integer
      int intValue;
      try
      {
         intValue = Integer.parseInt(value.trim());
      }
      catch(NumberFormatException e)
      {
         return ValidationResult.failure("Must be a valid integer");
      }

      // Min validation
      if(validation.min() != null && intValue < validation.min())
      {
         String message = validation.message() != null
            ? validation.message()
            : "Must be at least " + validation.min();
         return ValidationResult.failure(message);
      }

      // Max validation
      if(validation.max() != null && intValue > validation.max())
      {
         String message = validation.message() != null
            ? validation.message()
            : "Must be at most " + validation.max();
         return ValidationResult.failure(message);
      }

      return ValidationResult.success();
   }



   /***************************************************************************
    * Validate against a regex pattern.
    *
    * @param pattern the regex pattern
    * @param value the value to validate
    * @param customMessage custom error message
    * @return validation result if failed, empty if passed
    * @since 0.2.0
    ***************************************************************************/
   private Optional<ValidationResult> validatePattern(String pattern, String value, String customMessage)
   {
      try
      {
         Pattern regex = Pattern.compile(pattern);
         if(!regex.matcher(value).matches())
         {
            String message = customMessage != null
               ? customMessage
               : "Must match pattern: " + pattern;
            return Optional.of(ValidationResult.failure(message));
         }
      }
      catch(PatternSyntaxException e)
      {
         return Optional.of(ValidationResult.failure("Invalid validation pattern: " + pattern));
      }
      return Optional.empty();
   }
}
