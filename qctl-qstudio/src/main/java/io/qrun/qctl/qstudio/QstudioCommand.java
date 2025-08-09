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
package io.qrun.qctl.qstudio;

import picocli.CommandLine.Command;

@Command(
    name = "qstudio",
    description = "qStudio planning commands",
    mixinStandardHelpOptions = true)
public class QstudioCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("qstudio: run a subcommand. Try --help.");
  }
}
