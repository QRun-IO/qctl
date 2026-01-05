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


/*******************************************************************************
 * Velocity tool for string transformations.
 *
 * Provides methods for common case conversions used in templates.
 * Exposed in Velocity context as $str.
 *
 * @since 0.2.0
 *******************************************************************************/
public class StringTool
{


   /***************************************************************************
    * Convert string to lowercase.
    *
    * @param value input string
    * @return lowercase string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String lower(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return value.toLowerCase();
   }



   /***************************************************************************
    * Convert string to uppercase.
    *
    * @param value input string
    * @return uppercase string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String upper(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return value.toUpperCase();
   }



   /***************************************************************************
    * Convert string to camelCase (first letter lowercase).
    *
    * @param value input string
    * @return camelCase string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String camel(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return Character.toLowerCase(value.charAt(0)) + value.substring(1);
   }



   /***************************************************************************
    * Convert string to PascalCase (first letter uppercase).
    *
    * @param value input string
    * @return PascalCase string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String pascal(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return Character.toUpperCase(value.charAt(0)) + value.substring(1);
   }



   /***************************************************************************
    * Convert camelCase to kebab-case.
    *
    * @param value input string in camelCase or PascalCase
    * @return kebab-case string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String kebab(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return value.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
   }



   /***************************************************************************
    * Convert camelCase to snake_case.
    *
    * @param value input string in camelCase or PascalCase
    * @return snake_case string, or empty string if null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String snake(String value)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
   }



   /***************************************************************************
    * Replace all occurrences of target with replacement.
    *
    * @param value input string
    * @param target string to find
    * @param replacement string to replace with
    * @return string with replacements, or empty if input is null/empty
    * @since 0.2.0
    ***************************************************************************/
   public String replace(String value, String target, String replacement)
   {
      if(value == null || value.isEmpty())
      {
         return "";
      }
      if(target == null || replacement == null)
      {
         return value;
      }
      return value.replace(target, replacement);
   }
}
