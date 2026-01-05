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


/*******************************************************************************
 * Structured error with category, message, and recovery suggestions.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TemplateError extends RuntimeException
{
   private final ErrorCategory category;
   private final List<String> suggestions;
   private final String hint;



   /***************************************************************************
    * Error categories mapping to exit codes.
    *
    * @since 0.2.0
    ***************************************************************************/
   public enum ErrorCategory
   {
      TEMPLATE_NOT_FOUND(ExitCodes.NOT_FOUND),
      VARIABLE_NOT_FOUND(ExitCodes.VALIDATION),
      MANIFEST_INVALID(ExitCodes.VALIDATION),
      RENDER_ERROR(ExitCodes.VALIDATION),
      TRANSFORM_ERROR(ExitCodes.VALIDATION),
      NETWORK_ERROR(ExitCodes.NETWORK),
      VERSION_MISMATCH(ExitCodes.VALIDATION),
      DIRECTORY_EXISTS(ExitCodes.CONFLICT),
      IO_ERROR(ExitCodes.GENERIC);

      private final int exitCode;



      /*************************************************************************
       * Create an error category with exit code.
       *
       * @param exitCode the exit code for this category
       * @since 0.2.0
       *************************************************************************/
      ErrorCategory(int exitCode)
      {
         this.exitCode = exitCode;
      }



      /*************************************************************************
       * Get the exit code for this category.
       *
       * @return exit code
       * @since 0.2.0
       *************************************************************************/
      public int getExitCode()
      {
         return exitCode;
      }
   }



   /***************************************************************************
    * Create a template error.
    *
    * @param category error category
    * @param message error message
    * @param suggestions list of suggestions (e.g., similar names)
    * @param hint actionable hint for recovery
    * @since 0.2.0
    ***************************************************************************/
   public TemplateError(ErrorCategory category, String message, List<String> suggestions, String hint)
   {
      super(message);
      this.category = category;
      this.suggestions = suggestions != null ? suggestions : List.of();
      this.hint = hint;
   }



   /***************************************************************************
    * Create a template error without suggestions.
    *
    * @param category error category
    * @param message error message
    * @param hint actionable hint
    * @since 0.2.0
    ***************************************************************************/
   public TemplateError(ErrorCategory category, String message, String hint)
   {
      this(category, message, null, hint);
   }



   /***************************************************************************
    * Create a template error with only message.
    *
    * @param category error category
    * @param message error message
    * @since 0.2.0
    ***************************************************************************/
   public TemplateError(ErrorCategory category, String message)
   {
      this(category, message, null, null);
   }



   /***************************************************************************
    * Create a template error wrapping a cause.
    *
    * @param category error category
    * @param message error message
    * @param cause the underlying cause
    * @since 0.2.0
    ***************************************************************************/
   public TemplateError(ErrorCategory category, String message, Throwable cause)
   {
      super(message, cause);
      this.category = category;
      this.suggestions = List.of();
      this.hint = null;
   }



   /***************************************************************************
    * Get the error category.
    *
    * @return error category
    * @since 0.2.0
    ***************************************************************************/
   public ErrorCategory getCategory()
   {
      return category;
   }



   /***************************************************************************
    * Get the exit code for this error.
    *
    * @return exit code
    * @since 0.2.0
    ***************************************************************************/
   public int getExitCode()
   {
      return category.getExitCode();
   }



   /***************************************************************************
    * Get suggestions for recovery.
    *
    * @return list of suggestions
    * @since 0.2.0
    ***************************************************************************/
   public List<String> getSuggestions()
   {
      return suggestions;
   }



   /***************************************************************************
    * Get actionable hint.
    *
    * @return hint or null
    * @since 0.2.0
    ***************************************************************************/
   public String getHint()
   {
      return hint;
   }



   /***************************************************************************
    * Check if error has suggestions.
    *
    * @return true if suggestions available
    * @since 0.2.0
    ***************************************************************************/
   public boolean hasSuggestions()
   {
      return !suggestions.isEmpty();
   }
}
