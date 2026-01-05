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


/*******************************************************************************
 * Exception thrown when template rendering fails.
 *
 * Provides context about the template error including location information
 * when available from the underlying template engine.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TemplateRenderException extends Exception
{
   private final String undefinedVariable;
   private final List<String> suggestions;
   private final List<String> availableVariables;
   private final String filePath;
   private final Integer lineNumber;



   /***************************************************************************
    * Create a new exception with a message.
    *
    * @param message error message
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message)
   {
      super(message);
      this.undefinedVariable = null;
      this.suggestions = List.of();
      this.availableVariables = List.of();
      this.filePath = null;
      this.lineNumber = null;
   }



   /***************************************************************************
    * Create a new exception with a message and cause.
    *
    * @param message error message
    * @param cause underlying cause
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message, Throwable cause)
   {
      super(message, cause);
      this.undefinedVariable = null;
      this.suggestions = List.of();
      this.availableVariables = List.of();
      this.filePath = null;
      this.lineNumber = null;
   }



   /***************************************************************************
    * Create exception for undefined variable with suggestions.
    *
    * @param undefinedVariable the undefined variable name
    * @param suggestions similar variable names
    * @param availableVariables all available variable names
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String undefinedVariable, List<String> suggestions,
                                   List<String> availableVariables)
   {
      super(buildMessage(undefinedVariable, suggestions, availableVariables));
      this.undefinedVariable = undefinedVariable;
      this.suggestions = suggestions != null ? suggestions : List.of();
      this.availableVariables = availableVariables != null ? availableVariables : List.of();
      this.filePath = null;
      this.lineNumber = null;
   }



   /***************************************************************************
    * Create exception with file path context.
    *
    * @param message error message
    * @param filePath path to the file being processed
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message, String filePath)
   {
      super(message + " (in " + filePath + ")");
      this.undefinedVariable = null;
      this.suggestions = List.of();
      this.availableVariables = List.of();
      this.filePath = filePath;
      this.lineNumber = null;
   }



   /***************************************************************************
    * Create exception with file path and line number.
    *
    * @param message error message
    * @param filePath path to the file
    * @param lineNumber line number where error occurred
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message, String filePath, Integer lineNumber)
   {
      super(buildMessageWithLocation(message, filePath, lineNumber));
      this.undefinedVariable = null;
      this.suggestions = List.of();
      this.availableVariables = List.of();
      this.filePath = filePath;
      this.lineNumber = lineNumber;
   }



   /***************************************************************************
    * Build error message for undefined variable.
    *
    * @param varName undefined variable name
    * @param suggestions similar variable names
    * @param available all available variables
    * @return formatted error message
    * @since 0.2.0
    ***************************************************************************/
   private static String buildMessage(String varName, List<String> suggestions,
                                        List<String> available)
   {
      StringBuilder sb = new StringBuilder();
      sb.append("Undefined variable: ").append(varName);

      if(suggestions != null && !suggestions.isEmpty())
      {
         sb.append("\n\nDid you mean?");
         for(String s : suggestions)
         {
            sb.append("\n  - ").append(s);
         }
      }

      if(available != null && !available.isEmpty())
      {
         sb.append("\n\nAvailable variables:");
         for(String v : available)
         {
            sb.append("\n  - ").append(v);
         }
      }

      sb.append("\n\nTip: Variable names are case-sensitive");
      return sb.toString();
   }



   /***************************************************************************
    * Build error message with file location.
    *
    * @param message base message
    * @param filePath file path
    * @param lineNumber line number (may be null)
    * @return formatted message with location
    * @since 0.2.0
    ***************************************************************************/
   private static String buildMessageWithLocation(String message, String filePath,
                                                    Integer lineNumber)
   {
      if(filePath == null)
      {
         return message;
      }
      if(lineNumber != null)
      {
         return message + " (" + filePath + ":" + lineNumber + ")";
      }
      return message + " (in " + filePath + ")";
   }



   /***************************************************************************
    * Get the undefined variable name if this is an undefined variable error.
    *
    * @return variable name or null
    * @since 0.2.0
    ***************************************************************************/
   public String getUndefinedVariable()
   {
      return undefinedVariable;
   }



   /***************************************************************************
    * Get suggestions for similar variable names.
    *
    * @return list of suggestions (may be empty)
    * @since 0.2.0
    ***************************************************************************/
   public List<String> getSuggestions()
   {
      return suggestions;
   }



   /***************************************************************************
    * Get all available variable names.
    *
    * @return list of available variables (may be empty)
    * @since 0.2.0
    ***************************************************************************/
   public List<String> getAvailableVariables()
   {
      return availableVariables;
   }



   /***************************************************************************
    * Get the file path where error occurred.
    *
    * @return file path or null
    * @since 0.2.0
    ***************************************************************************/
   public String getFilePath()
   {
      return filePath;
   }



   /***************************************************************************
    * Get the line number where error occurred.
    *
    * @return line number or null
    * @since 0.2.0
    ***************************************************************************/
   public Integer getLineNumber()
   {
      return lineNumber;
   }
}
