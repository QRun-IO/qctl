package io.qrun.qctl.qrun;

import io.qrun.qctl.core.http.ApiClient;
import io.qrun.qctl.core.sys.SystemPaths;
import java.net.URI;
import java.time.Duration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "status", description = "Show app status from qRun API (mock)")
public class StatusCommand implements Runnable {
  @Option(names = "--app", required = true)
  String app;
  @Option(names = "--env", required = true)
  String env;

  @Override
  public void run() {
    try {
      var store = new io.qrun.qctl.core.auth.TokenStore(SystemPaths.configDir());
      var apiKey = store.readApiKey();
      ApiClient client = new ApiClient(Duration.ofSeconds(30), builder -> {
        builder.header("User-Agent", "qctl/0.1.0");
        apiKey.ifPresent(k -> builder.header("X-API-Key", k));
      });
      URI uri = URI.create("http://localhost:4010/v1/apps/" + app + "/status?env=" + env);
      var status = client.getJson(uri, java.util.Map.class);
      System.out.println(status);
    } catch (ApiClient.ApiException e) {
      System.err.println("error: " + e.problem.title + ": " + e.problem.detail);
      System.exit(e.exitCode);
    } catch (Exception e) {
      System.err.println("error: " + e.getMessage());
      System.exit(1);
    }
  }
}
