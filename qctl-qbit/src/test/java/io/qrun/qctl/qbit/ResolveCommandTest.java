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
package io.qrun.qctl.qbit;


import java.nio.file.Files;
import java.nio.file.Path;
import io.qrun.qctl.qbit.lock.Lockfile;
import io.qrun.qctl.qbit.lock.LockfileIO;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class ResolveCommandTest
{
   @Test
   void summary_counts_packages() throws Exception
   {
      assertThat(ResolveCommand.summary(0)).contains("0");
      assertThat(ResolveCommand.summary(3)).contains("3");
   }



   @Test
   void hermetic_reads_lockfile() throws Exception
   {
      Path                  tmp = Files.createTempFile("qbits", ".lock");
      Lockfile              lf  = new Lockfile();
      Lockfile.PackageEntry e   = new Lockfile.PackageEntry();
      e.name = "io.qbits/auth";
      e.version = "2.3.1";
      e.resolved = "https://registry.qrun.io/io.qbits/auth/2.3.1.tgz";
      e.integrity = "sha512-b8c1...";
      lf.packages.put(e.name, e);
      LockfileIO.writeAtomic(tmp, lf);

      Lockfile re = LockfileIO.read(tmp);
      assertThat(re.packages).hasSize(1);
   }
}
