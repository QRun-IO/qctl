package io.qrun.qctl.core.cli.auth;

import io.qrun.qctl.core.auth.TokenStore;
import io.qrun.qctl.core.sys.SystemPaths;
import java.util.Optional;
import picocli.CommandLine.Command;

@Command(name = "whoami", description = "Show current identity (V1 stub)")
public class WhoamiCommand implements Runnable {
  @Override
  public void run() {
    Optional<String> apiKey = new TokenStore(SystemPaths.configDir()).readApiKey();
    System.out.println("api-key: " + (apiKey.isPresent() ? "set" : "not set"));
  }
}
