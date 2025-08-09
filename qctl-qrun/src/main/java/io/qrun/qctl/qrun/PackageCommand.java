package io.qrun.qctl.qrun;

import io.qrun.qctl.core.output.Output;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import picocli.CommandLine.Command;

@Command(name = "package", description = "Prepare dummy artifact manifest (mock V1)")
public class PackageCommand implements Runnable {
  @Override
  public void run() {
    try {
      Path target = Path.of("target");
      Files.createDirectories(target);
      Path manifest = target.resolve("artifact-manifest.json");
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("kind", "oci");
      m.put("digest", "sha256:d34db33f");
      m.put("sizeBytes", 12345678);
      m.put("createdAt", Instant.now().toString());
      String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
      Files.writeString(manifest, json);
      Output.text(System.out, "wrote " + manifest);
    } catch (IOException e) {
      System.err.println("qrun package error: " + e.getMessage());
      System.exit(1);
    }
  }
}
