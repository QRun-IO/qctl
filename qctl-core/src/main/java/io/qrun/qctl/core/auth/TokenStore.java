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
