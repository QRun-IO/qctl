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

package io.qrun.qctl.e2e.harness;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import io.qrun.qctl.qqq.template.ConsoleUI;


/*******************************************************************************
 * Test-friendly ConsoleUI that queues inputs and captures outputs.
 *
 * Allows tests to pre-populate input responses and verify prompt sequences
 * without actual user interaction.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TestableConsoleUI extends ConsoleUI
{
   private final Queue<String> queuedInputs = new LinkedList<>();
   private final List<String> promptHistory = new ArrayList<>();
   private final List<String> outputHistory = new ArrayList<>();



   /***************************************************************************
    * Queue an input value to be returned by the next prompt.
    *
    * @param input the input value
    * @return this for fluent chaining
    * @since 0.2.0
    ***************************************************************************/
   public TestableConsoleUI queueInput(String input)
   {
      queuedInputs.add(input);
      return this;
   }



   /***************************************************************************
    * Queue multiple input values in order.
    *
    * @param inputs the input values
    * @return this for fluent chaining
    * @since 0.2.0
    ***************************************************************************/
   public TestableConsoleUI queueInputs(String... inputs)
   {
      for(String input : inputs)
      {
         queuedInputs.add(input);
      }
      return this;
   }



   /***************************************************************************
    * Get the list of prompts that were displayed.
    *
    * @return list of prompt labels
    * @since 0.2.0
    ***************************************************************************/
   public List<String> getPromptHistory()
   {
      return new ArrayList<>(promptHistory);
   }



   /***************************************************************************
    * Get the list of outputs (headers, info, success, etc).
    *
    * @return list of output messages
    * @since 0.2.0
    ***************************************************************************/
   public List<String> getOutputHistory()
   {
      return new ArrayList<>(outputHistory);
   }



   /***************************************************************************
    * Reset all queued inputs and histories.
    *
    * @return this for fluent chaining
    * @since 0.2.0
    ***************************************************************************/
   public TestableConsoleUI reset()
   {
      queuedInputs.clear();
      promptHistory.clear();
      outputHistory.clear();
      return this;
   }



   /***************************************************************************
    * Check if any queued inputs remain unused.
    *
    * @return true if inputs remain
    * @since 0.2.0
    ***************************************************************************/
   public boolean hasUnusedInputs()
   {
      return !queuedInputs.isEmpty();
   }



   /***************************************************************************
    * Get next queued input or empty string if none.
    *
    * @return queued input
    * @since 0.2.0
    ***************************************************************************/
   private String nextInput()
   {
      return queuedInputs.poll();
   }



   //////////////////////////////////////////////////////////////////////////
   // Overridden methods to capture and simulate                          //
   //////////////////////////////////////////////////////////////////////////

   @Override
   public void header(String title)
   {
      outputHistory.add("HEADER: " + title);
   }



   @Override
   public void subtitle(String text)
   {
      outputHistory.add("SUBTITLE: " + text);
   }



   @Override
   public void info(String text)
   {
      outputHistory.add("INFO: " + text);
   }



   @Override
   public void success(String text)
   {
      outputHistory.add("SUCCESS: " + text);
   }



   @Override
   public void successBold(String text)
   {
      outputHistory.add("SUCCESS_BOLD: " + text);
   }



   @Override
   public void error(String text)
   {
      outputHistory.add("ERROR: " + text);
   }



   @Override
   public void validationError(String text)
   {
      outputHistory.add("VALIDATION_ERROR: " + text);
   }



   @Override
   public void validationSuccess()
   {
      outputHistory.add("VALIDATION_SUCCESS");
   }



   @Override
   public void warning(String text)
   {
      outputHistory.add("WARNING: " + text);
   }



   @Override
   public void println()
   {
      outputHistory.add("PRINTLN:");
   }



   @Override
   public void println(String text)
   {
      outputHistory.add("PRINTLN: " + text);
   }



   @Override
   public String promptText(String label, String defaultValue)
   {
      promptHistory.add(label);
      String input = nextInput();
      if(input == null || input.isEmpty())
      {
         return defaultValue != null ? defaultValue : "";
      }
      return input;
   }



   @Override
   public void showValue(String label, String value)
   {
      outputHistory.add("SHOW_VALUE: " + label + "=" + value);
   }



   @Override
   public void showValue(String label, String value, String annotation)
   {
      outputHistory.add("SHOW_VALUE: " + label + "=" + value + " (" + annotation + ")");
   }



   @Override
   public <T> T promptSelect(String label,
                             List<T> items,
                             Function<T, String> displayFn,
                             Function<T, String> descriptionFn,
                             int defaultIndex)
   {
      promptHistory.add(label);
      String input = nextInput();

      int selectedIndex = defaultIndex;
      if(input != null && !input.isEmpty())
      {
         try
         {
            int parsed = Integer.parseInt(input.trim()) - 1;
            if(parsed >= 0 && parsed < items.size())
            {
               selectedIndex = parsed;
            }
         }
         catch(NumberFormatException e)
         {
            // Use default
         }
      }

      return items.get(selectedIndex);
   }
}
