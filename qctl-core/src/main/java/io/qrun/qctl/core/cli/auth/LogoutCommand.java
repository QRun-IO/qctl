package io.qrun.qctl.core.cli.auth;

import io.qrun.qctl.core.auth.TokenStore;
import io.qrun.qctl.core.sys.SystemPaths;
import picocli.CommandLine.Command;

@Command(name = "logout", description = "Remove stored credentials (V1)")
public class LogoutCommand implements Runnable {
  @Override
  public void run() {
    try {
      new TokenStore(SystemPaths.configDir()).clear();
      System.out.println("auth logout: removed stored token");
    } catch (Exception e) {
      System.err.println("auth logout error: " + e.getMessage());
      System.exit(1);
    }
  }
}
