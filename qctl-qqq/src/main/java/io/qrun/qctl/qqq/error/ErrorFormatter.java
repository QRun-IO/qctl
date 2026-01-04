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


import java.io.PrintWriter;
import java.io.StringWriter;


/*******************************************************************************
 * Formats errors with consistent styling and recovery suggestions.
 *
 * @since 0.2.0
 *******************************************************************************/
public class ErrorFormatter
{
   private static final String ANSI_RED = "\u001B[31m";
   private static final String ANSI_YELLOW = "\u001B[33m";
   private static final String ANSI_CYAN = "\u001B[36m";
   private static final String ANSI_DIM = "\u001B[2m";
   private static final String ANSI_RESET = "\u001B[0m";

   private final boolean verbose;



   /***************************************************************************
    * Create an error formatter.
    *
    * @param verbose whether to include stack traces
    * @since 0.2.0
    ***************************************************************************/
   public ErrorFormatter(boolean verbose)
   {
      this.verbose = verbose;
   }



   /***************************************************************************
    * Format a template error for display.
    *
    * @param error the template error
    * @return formatted error message
    * @since 0.2.0
    ***************************************************************************/
   public String format(TemplateError error)
   {
      StringBuilder sb = new StringBuilder();

      // Error message
      sb.append(ANSI_RED).append("error: ").append(ANSI_RESET);
      sb.append(error.getMessage()).append("\n");

      // Suggestions (did you mean?)
      if(error.hasSuggestions())
      {
         sb.append("\n");
         sb.append(ANSI_YELLOW).append("Did you mean?").append(ANSI_RESET).append("\n");
         for(String suggestion : error.getSuggestions())
         {
            sb.append("  ").append(ANSI_CYAN).append(suggestion).append(ANSI_RESET).append("\n");
         }
      }

      // Hint
      if(error.getHint() != null)
      {
         sb.append("\n");
         sb.append(ANSI_DIM).append(error.getHint()).append(ANSI_RESET).append("\n");
      }

      // Stack trace (verbose mode)
      if(verbose && error.getCause() != null)
      {
         sb.append("\n");
         sb.append(ANSI_DIM).append("Stack trace:").append(ANSI_RESET).append("\n");
         sb.append(getStackTrace(error.getCause()));
      }

      return sb.toString();
   }



   /***************************************************************************
    * Format a generic exception.
    *
    * @param error the exception
    * @param hint optional hint for recovery
    * @return formatted error message
    * @since 0.2.0
    ***************************************************************************/
   public String format(Exception error, String hint)
   {
      StringBuilder sb = new StringBuilder();

      sb.append(ANSI_RED).append("error: ").append(ANSI_RESET);
      sb.append(error.getMessage()).append("\n");

      if(hint != null)
      {
         sb.append("\n");
         sb.append(ANSI_DIM).append(hint).append(ANSI_RESET).append("\n");
      }

      if(verbose)
      {
         sb.append("\n");
         sb.append(ANSI_DIM).append("Stack trace:").append(ANSI_RESET).append("\n");
         sb.append(getStackTrace(error));
      }

      return sb.toString();
   }



   /***************************************************************************
    * Get stack trace as string.
    *
    * @param throwable the exception
    * @return stack trace string
    * @since 0.2.0
    ***************************************************************************/
   private String getStackTrace(Throwable throwable)
   {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwable.printStackTrace(pw);
      return sw.toString();
   }
}
