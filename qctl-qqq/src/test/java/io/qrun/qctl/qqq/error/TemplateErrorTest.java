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

package io.qrun.qctl.qqq.error;


import java.util.List;
import io.qrun.qctl.shared.ExitCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Tests for TemplateError structured error handling.
 *
 * @since 0.2.0
 *******************************************************************************/
class TemplateErrorTest
{

   /***************************************************************************
    * Test error with all fields.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testConstructor_allFields()
   {
      List<String> suggestions = List.of("foo", "bar");
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.TEMPLATE_NOT_FOUND,
         "Template not found",
         suggestions,
         "Check the template name"
      );

      assertThat(error.getCategory()).isEqualTo(TemplateError.ErrorCategory.TEMPLATE_NOT_FOUND);
      assertThat(error.getMessage()).isEqualTo("Template not found");
      assertThat(error.getSuggestions()).containsExactly("foo", "bar");
      assertThat(error.getHint()).isEqualTo("Check the template name");
      assertThat(error.hasSuggestions()).isTrue();
   }



   /***************************************************************************
    * Test error with only message and hint.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testConstructor_messageAndHint()
   {
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.RENDER_ERROR,
         "Failed to render",
         "Check your syntax"
      );

      assertThat(error.getMessage()).isEqualTo("Failed to render");
      assertThat(error.getHint()).isEqualTo("Check your syntax");
      assertThat(error.getSuggestions()).isEmpty();
      assertThat(error.hasSuggestions()).isFalse();
   }



   /***************************************************************************
    * Test error with only message.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testConstructor_messageOnly()
   {
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.IO_ERROR,
         "File not readable"
      );

      assertThat(error.getMessage()).isEqualTo("File not readable");
      assertThat(error.getHint()).isNull();
      assertThat(error.getSuggestions()).isEmpty();
   }



   /***************************************************************************
    * Test error wrapping a cause.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testConstructor_withCause()
   {
      Exception cause = new RuntimeException("Root cause");
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.NETWORK_ERROR,
         "Connection failed",
         cause
      );

      assertThat(error.getMessage()).isEqualTo("Connection failed");
      assertThat(error.getCause()).isEqualTo(cause);
   }



   /***************************************************************************
    * Test exit code mapping for each category.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testExitCodes()
   {
      assertThat(new TemplateError(TemplateError.ErrorCategory.TEMPLATE_NOT_FOUND, "x").getExitCode())
         .isEqualTo(ExitCodes.NOT_FOUND);

      assertThat(new TemplateError(TemplateError.ErrorCategory.VARIABLE_NOT_FOUND, "x").getExitCode())
         .isEqualTo(ExitCodes.VALIDATION);

      assertThat(new TemplateError(TemplateError.ErrorCategory.MANIFEST_INVALID, "x").getExitCode())
         .isEqualTo(ExitCodes.VALIDATION);

      assertThat(new TemplateError(TemplateError.ErrorCategory.RENDER_ERROR, "x").getExitCode())
         .isEqualTo(ExitCodes.VALIDATION);

      assertThat(new TemplateError(TemplateError.ErrorCategory.NETWORK_ERROR, "x").getExitCode())
         .isEqualTo(ExitCodes.NETWORK);

      assertThat(new TemplateError(TemplateError.ErrorCategory.DIRECTORY_EXISTS, "x").getExitCode())
         .isEqualTo(ExitCodes.CONFLICT);

      assertThat(new TemplateError(TemplateError.ErrorCategory.IO_ERROR, "x").getExitCode())
         .isEqualTo(ExitCodes.GENERIC);
   }



   /***************************************************************************
    * Test null suggestions becomes empty list.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testNullSuggestions_becomesEmptyList()
   {
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.RENDER_ERROR,
         "Error",
         null,
         "hint"
      );

      assertThat(error.getSuggestions()).isEmpty();
      assertThat(error.hasSuggestions()).isFalse();
   }
}
