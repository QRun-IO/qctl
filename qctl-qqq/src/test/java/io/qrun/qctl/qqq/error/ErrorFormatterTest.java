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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Tests for ErrorFormatter output formatting.
 *
 * @since 0.2.0
 *******************************************************************************/
class ErrorFormatterTest
{

   /***************************************************************************
    * Test basic error formatting.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_basicError()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.RENDER_ERROR,
         "Syntax error in template"
      );

      String output = formatter.format(error);

      assertThat(output).contains("error:");
      assertThat(output).contains("Syntax error in template");
   }



   /***************************************************************************
    * Test error with suggestions.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_withSuggestions()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.TEMPLATE_NOT_FOUND,
         "Template 'foo' not found",
         List.of("foobar", "foo-api"),
         null
      );

      String output = formatter.format(error);

      assertThat(output).contains("Did you mean?");
      assertThat(output).contains("foobar");
      assertThat(output).contains("foo-api");
   }



   /***************************************************************************
    * Test error with hint.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_withHint()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.NETWORK_ERROR,
         "Connection refused",
         "Check your network connection"
      );

      String output = formatter.format(error);

      assertThat(output).contains("Check your network connection");
   }



   /***************************************************************************
    * Test verbose mode includes stack trace.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_verboseWithCause()
   {
      ErrorFormatter formatter = new ErrorFormatter(true);
      Exception cause = new RuntimeException("Root cause");
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.IO_ERROR,
         "Failed to read file",
         cause
      );

      String output = formatter.format(error);

      assertThat(output).contains("Stack trace:");
      assertThat(output).contains("RuntimeException");
      assertThat(output).contains("Root cause");
   }



   /***************************************************************************
    * Test non-verbose mode hides stack trace.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_nonVerboseHidesStackTrace()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      Exception cause = new RuntimeException("Root cause");
      TemplateError error = new TemplateError(
         TemplateError.ErrorCategory.IO_ERROR,
         "Failed to read file",
         cause
      );

      String output = formatter.format(error);

      assertThat(output).doesNotContain("Stack trace:");
   }



   /***************************************************************************
    * Test formatting generic exception.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_genericException()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      Exception e = new RuntimeException("Something went wrong");

      String output = formatter.format(e, "Try again later");

      assertThat(output).contains("error:");
      assertThat(output).contains("Something went wrong");
      assertThat(output).contains("Try again later");
   }



   /***************************************************************************
    * Test formatting exception without hint.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_exceptionNoHint()
   {
      ErrorFormatter formatter = new ErrorFormatter(false);
      Exception e = new RuntimeException("Error message");

      String output = formatter.format(e, null);

      assertThat(output).contains("error:");
      assertThat(output).contains("Error message");
   }



   /***************************************************************************
    * Test verbose exception shows stack trace.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Test
   void testFormat_verboseException()
   {
      ErrorFormatter formatter = new ErrorFormatter(true);
      Exception e = new RuntimeException("Error");

      String output = formatter.format(e, null);

      assertThat(output).contains("Stack trace:");
   }
}
