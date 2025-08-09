package io.qrun.qctl.qstudio;

import picocli.CommandLine.Command;

@Command(name = "qstudio", description = "qStudio planning commands", mixinStandardHelpOptions = true)
public class QstudioCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("qstudio: run a subcommand. Try --help.");
  }
}
