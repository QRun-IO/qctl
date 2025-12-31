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
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;


/*******************************************************************************
 * Console UI utilities for interactive prompts.
 *
 * Provides consistent styling and input handling for CLI interactions.
 * Falls back to stdin when console is unavailable.
 *
 * @since 0.1.0
 *******************************************************************************/
public class ConsoleUI
{
   private static final String ANSI_BOLD = "\u001B[1m";
   private static final String ANSI_CYAN = "\u001B[36m";
   private static final String ANSI_RESET = "\u001B[0m";
   private static final String ANSI_DIM = "\u001B[2m";
   private static final String ANSI_GREEN = "\u001B[32m";

   private final Console console;
   private final Scanner scanner;



   /***************************************************************************
    * Create a new ConsoleUI.
    *
    * @since 0.1.0
    ***************************************************************************/
   public ConsoleUI()
   {
      this.console = System.console();
      this.scanner = new Scanner(System.in);
   }



   /***************************************************************************
    * Print a header line.
    *
    * @param title the title text
    * @since 0.1.0
    ***************************************************************************/
   public void header(String title)
   {
      System.out.println();
      System.out.println(ANSI_BOLD + title + ANSI_RESET);
   }



   /***************************************************************************
    * Print a subtitle/description.
    *
    * @param text the subtitle text
    * @since 0.1.0
    ***************************************************************************/
   public void subtitle(String text)
   {
      System.out.println(ANSI_DIM + text + ANSI_RESET);
   }



   /***************************************************************************
    * Print an info message.
    *
    * @param text the message text
    * @since 0.1.0
    ***************************************************************************/
   public void info(String text)
   {
      System.out.println(ANSI_DIM + text + ANSI_RESET);
   }



   /***************************************************************************
    * Print a success message.
    *
    * @param text the message text
    * @since 0.1.0
    ***************************************************************************/
   public void success(String text)
   {
      System.out.println(ANSI_GREEN + text + ANSI_RESET);
   }



   /***************************************************************************
    * Print a bold success message.
    *
    * @param text the message text
    * @since 0.1.0
    ***************************************************************************/
   public void successBold(String text)
   {
      System.out.println(ANSI_GREEN + ANSI_BOLD + text + ANSI_RESET);
   }



   /***************************************************************************
    * Print an error message.
    *
    * @param text the message text
    * @since 0.1.0
    ***************************************************************************/
   public void error(String text)
   {
      System.err.println("error: " + text);
   }



   /***************************************************************************
    * Print a blank line.
    *
    * @since 0.1.0
    ***************************************************************************/
   public void println()
   {
      System.out.println();
   }



   /***************************************************************************
    * Print text.
    *
    * @param text the text to print
    * @since 0.1.0
    ***************************************************************************/
   public void println(String text)
   {
      System.out.println(text);
   }



   /***************************************************************************
    * Prompt for text input.
    *
    * @param label the prompt label
    * @param defaultValue default value (may be null)
    * @return user input or default
    * @since 0.1.0
    ***************************************************************************/
   public String promptText(String label, String defaultValue)
   {
      StringBuilder sb = new StringBuilder();
      sb.append(ANSI_CYAN).append("? ").append(ANSI_RESET);
      sb.append(ANSI_BOLD).append(label).append(ANSI_RESET);
      if(defaultValue != null)
      {
         sb.append(" ").append(ANSI_DIM).append("(").append(defaultValue).append(")").append(ANSI_RESET);
      }
      sb.append(": ");

      System.out.print(sb);

      String input = readLine();
      if(input == null || input.isBlank())
      {
         return defaultValue != null ? defaultValue : "";
      }
      return input.trim();
   }



   /***************************************************************************
    * Display a value that was pre-set (not prompted).
    *
    * @param label the label
    * @param value the value
    * @since 0.1.0
    ***************************************************************************/
   public void showValue(String label, String value)
   {
      System.out.println(ANSI_CYAN + "? " + ANSI_RESET + ANSI_BOLD + label + ANSI_RESET + ": " + value);
   }



   /***************************************************************************
    * Display a value with a dimmed annotation.
    *
    * @param label the label
    * @param value the value
    * @param annotation additional info in parentheses
    * @since 0.1.0
    ***************************************************************************/
   public void showValue(String label, String value, String annotation)
   {
      System.out.println(ANSI_CYAN + "? " + ANSI_RESET + ANSI_BOLD + label + ANSI_RESET
         + ": " + value + " " + ANSI_DIM + "(" + annotation + ")" + ANSI_RESET);
   }



   /***************************************************************************
    * Prompt for selection from a list.
    *
    * @param <T> the type of items
    * @param label the prompt label
    * @param items the list of items
    * @param displayFn function to get display text for each item
    * @param descriptionFn function to get description for each item (may return null)
    * @param defaultIndex default selection index (0-based)
    * @return selected item
    * @since 0.1.0
    ***************************************************************************/
   public <T> T promptSelect(String label,
                             List<T> items,
                             Function<T, String> displayFn,
                             Function<T, String> descriptionFn,
                             int defaultIndex)
   {
      System.out.println(ANSI_CYAN + "? " + ANSI_RESET + ANSI_BOLD + label + ANSI_RESET);

      for(int i = 0; i < items.size(); i++)
      {
         T item = items.get(i);
         String display = displayFn.apply(item);
         String desc = descriptionFn != null ? descriptionFn.apply(item) : null;
         boolean isDefault = (i == defaultIndex);

         StringBuilder sb = new StringBuilder();
         sb.append("  ").append(i + 1).append(") ").append(display);
         if(desc != null)
         {
            sb.append(ANSI_DIM).append(" - ").append(desc).append(ANSI_RESET);
         }
         if(isDefault)
         {
            sb.append(" (default)");
         }
         System.out.println(sb);
      }

      System.out.print("  Enter number [1-" + items.size() + "]: ");
      String input = readLine();

      int selectedIndex = defaultIndex;
      if(input != null && !input.isBlank())
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



   /***************************************************************************
    * Read a line from console or stdin.
    *
    * @return user input line
    * @since 0.1.0
    ***************************************************************************/
   private String readLine()
   {
      if(console != null)
      {
         return console.readLine();
      }
      else if(scanner.hasNextLine())
      {
         return scanner.nextLine();
      }
      return "";
   }
}
