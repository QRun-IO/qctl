package io.qrun.qctl.qbit.lock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LockfileIO {
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

  public static Lockfile read(Path path) throws IOException {
    if (!Files.exists(path)) return new Lockfile();
    return YAML.readValue(path.toFile(), Lockfile.class);
  }

  public static void writeAtomic(Path path, Lockfile lf) throws IOException {
    Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
    YAML.writeValue(tmp.toFile(), lf);
    Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private LockfileIO() {}
}
