package io.qrun.qctl.core.cli.auth;

import io.qrun.qctl.core.auth.TokenStore;
import io.qrun.qctl.core.sys.SystemPaths;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "login", description = "Start device login flow (stub)")
public class LoginCommand implements Runnable {
  @Option(names = "--api-key", description = "Set API key for mock/testing")
  String apiKey;
  @Override
  public void run() {
    try {
      if (apiKey != null && !apiKey.isBlank()) {
        new TokenStore(SystemPaths.configDir()).writeApiKey(apiKey.trim());
        System.out.println("auth login: API key stored (mock/testing)");
        return;
      }
      System.out.println("auth login: device code flow stub (V1)");
    } catch (Exception e) {
      System.err.println("auth login error: " + e.getMessage());
      System.exit(1);
    }
  }
}
