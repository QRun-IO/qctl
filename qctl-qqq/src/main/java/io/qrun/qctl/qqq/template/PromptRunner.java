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


import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 * Runs interactive prompts to collect template variables.
 *
 * Uses ConsoleUI for all interactive input. Supports text input and selections.
 *
 * @since 0.1.0
 *******************************************************************************/
public class PromptRunner
{
   private final ConsoleUI ui;



   /***************************************************************************
    * Create a new PromptRunner with default ConsoleUI.
    *
    * @since 0.1.0
    ***************************************************************************/
   public PromptRunner()
   {
      this.ui = new ConsoleUI();
   }



   /***************************************************************************
    * Create a new PromptRunner with provided ConsoleUI.
    *
    * @param ui the console UI to use
    * @since 0.1.0
    ***************************************************************************/
   public PromptRunner(ConsoleUI ui)
   {
      this.ui = ui;
   }



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

      ui.println();
      ui.header("Configure your project:");
      ui.println();

      for(TemplateManifest.Prompt prompt : prompts)
      {
         // Skip if already provided via CLI
         if(overrides.containsKey(prompt.name()))
         {
            String value = overrides.get(prompt.name());
            ui.showValue(prompt.message() != null ? prompt.message() : prompt.name(), value);
            results.put(prompt.name(), value);
            continue;
         }

         String value = promptForValue(prompt);
         results.put(prompt.name(), value);
      }

      return results;
   }



   /***************************************************************************
    * Prompt for a single value.
    *
    * @param prompt prompt definition
    * @return user input value
    * @since 0.1.0
    ***************************************************************************/
   private String promptForValue(TemplateManifest.Prompt prompt)
   {
      String type = prompt.type() != null ? prompt.type() : "text";
      String message = prompt.message() != null ? prompt.message() : prompt.name();

      if("select".equals(type) && prompt.choices() != null)
      {
         return promptSelect(message, prompt.choices(), prompt.defaultValue());
      }
      else
      {
         return ui.promptText(message, prompt.defaultValue());
      }
   }



   /***************************************************************************
    * Prompt for selection from choices.
    *
    * @param message the prompt message
    * @param choices list of choices
    * @param defaultValue default choice
    * @return selected value
    * @since 0.1.0
    ***************************************************************************/
   private String promptSelect(String message, List<String> choices, String defaultValue)
   {
      int defaultIndex = 0;
      if(defaultValue != null)
      {
         for(int i = 0; i < choices.size(); i++)
         {
            if(defaultValue.equals(choices.get(i)))
            {
               defaultIndex = i;
               break;
            }
         }
      }

      return ui.promptSelect(
         message,
         choices,
         s -> s,
         s -> null,
         defaultIndex
      );
   }
}
