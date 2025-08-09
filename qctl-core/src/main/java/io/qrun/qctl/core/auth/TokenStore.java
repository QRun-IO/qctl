package io.qrun.qctl.core.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class TokenStore {
  private final Path file;

  public TokenStore(Path configDir) {
    this.file = configDir.resolve("tokens.json");
  }

  public Optional<String> readApiKey() {
    try {
      if (!Files.exists(file)) return Optional.empty();
      String s = Files.readString(file).trim();
      if (s.isEmpty()) return Optional.empty();
      return Optional.of(s);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public void writeApiKey(String key) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, key);
  }

  public void clear() throws IOException {
    Files.deleteIfExists(file);
  }
}
