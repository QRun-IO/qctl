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

package io.qrun.qctl.qqq;


import java.io.IOException;
import java.util.List;
import io.qrun.qctl.qqq.registry.TemplateInfo;
import io.qrun.qctl.qqq.registry.TemplateRegistry;
import io.qrun.qctl.qqq.registry.TemplateRegistryFactory;
import io.qrun.qctl.shared.ExitCodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


/*******************************************************************************
 * List available project templates from the registry.
 *
 * @since 0.1.0
 *******************************************************************************/
@Command(name = "list", description = "List available project templates")
public class ListCommand implements Runnable
{
   private static final String ANSI_BOLD = "\u001B[1m";
   private static final String ANSI_CYAN = "\u001B[36m";
   private static final String ANSI_DIM = "\u001B[2m";
   private static final String ANSI_GREEN = "\u001B[32m";
   private static final String ANSI_RESET = "\u001B[0m";

   @Option(names = "--refresh", description = "Bypass cache and fetch fresh data")
   boolean refresh;



   /***************************************************************************
    * Default constructor for picocli.
    *
    * @since 0.1.0
    ***************************************************************************/
   public ListCommand()
   {
   }



   @Override
   public void run()
   {
      try
      {
         System.out.println("Fetching templates from registry...\n");
         TemplateRegistry registry = TemplateRegistryFactory.getRegistry();

         if(refresh)
         {
            registry.refresh();
         }

         List<TemplateInfo> templates = registry.listTemplates();

         if(templates.isEmpty())
         {
            System.out.println("No templates available.");
            return;
         }

         System.out.println(ANSI_BOLD + "Available Templates:" + ANSI_RESET + "\n");

         for(TemplateInfo template : templates)
         {
            System.out.println(ANSI_CYAN + "  " + template.id() + ANSI_RESET
               + "  " + ANSI_GREEN + "v" + template.version() + ANSI_RESET);
            System.out.println("    " + template.name());
            System.out.println("    " + ANSI_DIM + template.description() + ANSI_RESET);
            if(template.tags() != null && !template.tags().isEmpty())
            {
               System.out.println("    Tags: " + String.join(", ", template.tags()));
            }
            System.out.println();
         }

         System.out.println("Use: qctl qqq init <template-id> -o <target-dir>");
      }
      catch(IOException e)
      {
         System.err.println("error: Failed to fetch templates: " + e.getMessage());
         System.exit(ExitCodes.NETWORK);
      }
   }
}
