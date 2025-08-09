package io.qrun.qctl.qrun;

import io.qrun.qctl.core.http.ApiClient;
import io.qrun.qctl.core.sys.SystemPaths;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "publish", description = "Publish artifact and create release (mock)")
public class PublishCommand implements Runnable {
  @Option(names = "--env", required = true)
  String env;

  @Option(names = "--idempotency-key")
  String idempotencyKey;

  @Override
  public void run() {
    try {
      var store = new io.qrun.qctl.core.auth.TokenStore(SystemPaths.configDir());
      var apiKey = store.readApiKey();
      String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
      ApiClient client = createClient(builder -> {
        builder.header("User-Agent", "qctl/0.1.0");
        builder.header("Idempotency-Key", key);
        apiKey.ifPresent(k -> builder.header("X-API-Key", k));
      });

      // POST /v1/artifacts (mock)
      URI artifacts = URI.create("http://localhost:4010/v1/artifacts");
      Map<?,?> artifact = client.postJson(artifacts, Map.of(
          "id", "01J0M40G3SJ0QJ9E3V1QK8A3R2",
          "kind", "oci",
          "digest", "sha256:d34db33f",
          "sizeBytes", 12345678,
          "createdAt", "2025-01-15T12:00:00Z"
      ), Map.class);

      // POST /v1/releases (mock)
      URI releases = URI.create("http://localhost:4010/v1/releases");
      Map<?,?> release = client.postJson(releases, Map.of(
          "id", "01J0M41V9X5J2B4M5H0G7D2T1Q",
          "appName", "demo-app",
          "version", "0.1.0",
          "artifactId", artifact.get("id"),
          "channel", "stable",
          "createdAt", "2025-01-15T12:00:00Z"
      ), Map.class);

      System.out.println(release);
    } catch (ApiClient.ApiException e) {
      System.err.println("error: " + e.problem.title + ": " + e.problem.detail);
      System.exit(e.exitCode);
    } catch (Exception e) {
      System.err.println("error: " + e.getMessage());
      System.exit(1);
    }
  }

  // Visible for tests
  protected ApiClient createClient(ApiClient.HeaderProvider headerProvider) {
    return new ApiClient(Duration.ofSeconds(30), headerProvider);
  }
}
