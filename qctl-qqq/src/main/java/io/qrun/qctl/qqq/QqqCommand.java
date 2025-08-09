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


import picocli.CommandLine.Command;


@Command(name = "qqq", description = "Scaffolding commands", mixinStandardHelpOptions = true)
public class QqqCommand implements Runnable
{
   /***************************************************************************
    * Entry for scaffolding commands.
    *
    * Why: Groups template-driven project generation operations.
    * @since 0.1.0
    ***************************************************************************/
   @Override
   public void run()
   {
      System.out.println("qqq: run a subcommand. Try --help.");
   }
}
