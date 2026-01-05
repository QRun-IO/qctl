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

package io.qrun.qctl.core.cli;


import java.util.List;
import io.qrun.qctl.core.doctor.DependencyChecker;
import io.qrun.qctl.core.doctor.DependencyStatus;
import io.qrun.qctl.shared.ExitCodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


/*******************************************************************************
 * Check system dependencies and report status.
 *
 * Verifies that required tools (Java, Maven, Git) are installed and meet
 * version requirements. Reports optional tools (Docker) as well.
 *
 * @since 0.2.0
 *******************************************************************************/
@Command(
   name = "doctor",
   description = "Check system dependencies and configuration"
)
public class DoctorCommand implements Runnable
{
   private static final String ANSI_RESET  = "\u001B[0m";
   private static final String ANSI_GREEN  = "\u001B[32m";
   private static final String ANSI_RED    = "\u001B[31m";
   private static final String ANSI_YELLOW = "\u001B[33m";
   private static final String ANSI_BOLD   = "\u001B[1m";
   private static final String ANSI_DIM    = "\u001B[2m";

   private static final String CHECK_MARK = "✓";
   private static final String CROSS_MARK = "✗";
   private static final String WARN_MARK  = "!";

   @Option(names = {"-v", "--verbose"}, description = "Show detailed information")
   private boolean verbose;



   /***************************************************************************
    * Run the doctor command.
    *
    * @since 0.2.0
    ***************************************************************************/
   @Override
   public void run()
   {
      System.out.println();
      System.out.println(ANSI_BOLD + "qctl doctor" + ANSI_RESET);
      System.out.println(ANSI_DIM + "Checking system dependencies..." + ANSI_RESET);
      System.out.println();

      List<DependencyStatus> results = DependencyChecker.checkAll();

      int requiredMissing = 0;
      int optionalMissing = 0;

      for(DependencyStatus status : results)
      {
         printStatus(status);

         if(!status.isOk() && !status.isOptional())
         {
            requiredMissing++;
         }
         else if(status.isOptional())
         {
            optionalMissing++;
         }
      }

      System.out.println();

      if(requiredMissing > 0)
      {
         System.out.println(ANSI_RED + requiredMissing + " required "
            + (requiredMissing == 1 ? "dependency" : "dependencies")
            + " missing." + ANSI_RESET);
         System.out.println("Run " + ANSI_BOLD + "qctl doctor -v" + ANSI_RESET + " for installation hints.");
         System.exit(ExitCodes.VALIDATION);
      }
      else if(optionalMissing > 0)
      {
         System.out.println(ANSI_YELLOW + optionalMissing + " optional "
            + (optionalMissing == 1 ? "dependency" : "dependencies")
            + " missing." + ANSI_RESET);
         if(verbose)
         {
            System.out.println("Optional dependencies enable additional features but are not required.");
         }
      }
      else
      {
         System.out.println(ANSI_GREEN + "All dependencies satisfied!" + ANSI_RESET);
      }
   }



   /***************************************************************************
    * Print a single dependency status line.
    *
    * @param status dependency status to print
    * @since 0.2.0
    ***************************************************************************/
   private void printStatus(DependencyStatus status)
   {
      String icon;
      String color;

      switch(status.status())
      {
         case OK:
            icon = ANSI_GREEN + CHECK_MARK + ANSI_RESET;
            color = ANSI_RESET;
            break;
         case NOT_FOUND:
         case VERSION_MISMATCH:
         case ERROR:
            icon = ANSI_RED + CROSS_MARK + ANSI_RESET;
            color = ANSI_RED;
            break;
         case OPTIONAL_MISSING:
            icon = ANSI_YELLOW + WARN_MARK + ANSI_RESET;
            color = ANSI_YELLOW;
            break;
         default:
            icon = "?";
            color = ANSI_RESET;
      }

      StringBuilder line = new StringBuilder();
      line.append("  ").append(icon).append(" ");

      if(status.isOk())
      {
         line.append(ANSI_BOLD).append(status.name()).append(ANSI_RESET);
         line.append(" ").append(status.version());
         if(status.path() != null && verbose)
         {
            line.append(ANSI_DIM).append("  ").append(status.path()).append(ANSI_RESET);
         }
      }
      else
      {
         line.append(color).append(ANSI_BOLD).append(status.name()).append(ANSI_RESET);
         line.append(color).append(" ").append(status.getMessage()).append(ANSI_RESET);
      }

      System.out.println(line);

      if(verbose && status.hint() != null && !status.isOk())
      {
         System.out.println(ANSI_DIM + "    " + status.hint() + ANSI_RESET);
      }
   }
}
