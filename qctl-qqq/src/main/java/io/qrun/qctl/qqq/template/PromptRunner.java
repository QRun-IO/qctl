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


import java.io.Console;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/*******************************************************************************
 * Runs interactive prompts to collect template variables.
 *
 * Supports text input, selections, and validation.
 * Falls back to simple stdin when console is unavailable.
 *
 * @since 0.1.0
 *******************************************************************************/
public class PromptRunner
{
   private static final String ANSI_BOLD = "\u001B[1m";
   private static final String ANSI_CYAN = "\u001B[36m";
   private static final String ANSI_RESET = "\u001B[0m";
   private static final String ANSI_DIM = "\u001B[2m";



   /***************************************************************************
    * Run prompts and collect user input.
    *
    * @param prompts list of prompt definitions
    * @param overrides CLI-provided variable overrides
    * @return collected variables
    * @throws IOException if prompt interaction fails
    * @since 0.1.0
    ***************************************************************************/
   public Map<String, String> run(List<TemplateManifest.Prompt> prompts,
                                  Map<String, String> overrides) throws IOException
   {
      Map<String, String> results = new LinkedHashMap<>();
      Console             console = System.console();

      System.out.println("\n" + ANSI_BOLD + "Configure your project:" + ANSI_RESET + "\n");

      for(TemplateManifest.Prompt prompt : prompts)
      {
         // Skip if already provided via CLI
         if(overrides.containsKey(prompt.name()))
         {
            results.put(prompt.name(), overrides.get(prompt.name()));
            continue;
         }

         String value = promptForValue(prompt, console);
         results.put(prompt.name(), value);
      }

      return results;
   }



   /***************************************************************************
    * Prompt for a single value.
    *
    * @param prompt prompt definition
    * @param console system console (may be null)
    * @return user input value
    * @since 0.1.0
    ***************************************************************************/
   private String promptForValue(TemplateManifest.Prompt prompt, Console console)
   {
      String type = prompt.type() != null ? prompt.type() : "text";

      if("select".equals(type) && prompt.choices() != null)
      {
         return promptSelect(prompt, console);
      }
      else
      {
         return promptText(prompt, console);
      }
   }



   /***************************************************************************
    * Prompt for text input.
    *
    * @param prompt prompt definition
    * @param console system console
    * @return user input
    * @since 0.1.0
    ***************************************************************************/
   private String promptText(TemplateManifest.Prompt prompt, Console console)
   {
      String message = prompt.message() != null ? prompt.message() : prompt.name();
      String defaultVal = prompt.defaultValue();

      StringBuilder sb = new StringBuilder();
      sb.append(ANSI_CYAN).append("? ").append(ANSI_RESET);
      sb.append(ANSI_BOLD).append(message).append(ANSI_RESET);
      if(defaultVal != null)
      {
         sb.append(" ").append(ANSI_DIM).append("(").append(defaultVal).append(")").append(ANSI_RESET);
      }
      sb.append(": ");

      System.out.print(sb);

      String input = readLine(console);
      if(input == null || input.isBlank())
      {
         input = defaultVal != null ? defaultVal : "";
      }

      return input.trim();
   }



   /***************************************************************************
    * Prompt for selection from choices.
    *
    * @param prompt prompt definition
    * @param console system console
    * @return selected value
    * @since 0.1.0
    ***************************************************************************/
   private String promptSelect(TemplateManifest.Prompt prompt, Console console)
   {
      String       message = prompt.message() != null ? prompt.message() : prompt.name();
      List<String> choices = prompt.choices();

      System.out.println(ANSI_CYAN + "? " + ANSI_RESET + ANSI_BOLD + message + ANSI_RESET);
      for(int i = 0; i < choices.size(); i++)
      {
         String choice = choices.get(i);
         boolean isDefault = choice.equals(prompt.defaultValue());
         System.out.println("  " + (i + 1) + ") " + choice + (isDefault ? " (default)" : ""));
      }

      System.out.print("  Enter number [1-" + choices.size() + "]: ");
      String input = readLine(console);

      if(input == null || input.isBlank())
      {
         return prompt.defaultValue() != null ? prompt.defaultValue() : choices.get(0);
      }

      try
      {
         int index = Integer.parseInt(input.trim()) - 1;
         if(index >= 0 && index < choices.size())
         {
            return choices.get(index);
         }
      }
      catch(NumberFormatException e)
      {
         // Fall through to default
      }

      return prompt.defaultValue() != null ? prompt.defaultValue() : choices.get(0);
   }



   /***************************************************************************
    * Read a line from console or stdin.
    *
    * @param console system console (may be null)
    * @return user input line
    * @since 0.1.0
    ***************************************************************************/
   private String readLine(Console console)
   {
      if(console != null)
      {
         return console.readLine();
      }
      else
      {
         Scanner scanner = new Scanner(System.in);
         if(scanner.hasNextLine())
         {
            return scanner.nextLine();
         }
         return "";
      }
   }
}
