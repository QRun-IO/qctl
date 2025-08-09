package io.qrun.qctl.qbit.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LockfileIOTest {
  @Test
  void read_write_roundtrip() throws Exception {
    Path tmp = Files.createTempFile("qbits", ".lock");
    Lockfile lf = new Lockfile();
    Lockfile.PackageEntry e = new Lockfile.PackageEntry();
    e.name = "io.qbits/auth";
    e.version = "2.3.1";
    e.resolved = "https://registry.qrun.io/io.qbits/auth/2.3.1.tgz";
    e.integrity = "sha512-b8c1...";
    lf.packages.put(e.name, e);

    LockfileIO.writeAtomic(tmp, lf);
    Lockfile re = LockfileIO.read(tmp);

    assertThat(re.packages).containsKey("io.qbits/auth");
    assertThat(re.packages.get("io.qbits/auth").version).isEqualTo("2.3.1");
  }
}
