package io.qrun.qctl.core.cli;

import picocli.CommandLine.Command;

@Command(
    name = "auth",
    description = "Authenticate to qRun APIs (V1 stub)",
    mixinStandardHelpOptions = true,
    subcommands = {
      io.qrun.qctl.core.cli.auth.LoginCommand.class,
      io.qrun.qctl.core.cli.auth.LogoutCommand.class,
      io.qrun.qctl.core.cli.auth.WhoamiCommand.class
    })
public class AuthCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("auth: use subcommands (login|logout|whoami)");
  }
}
