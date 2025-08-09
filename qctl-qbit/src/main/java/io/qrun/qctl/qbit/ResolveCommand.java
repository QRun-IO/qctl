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

import io.qrun.qctl.qbit.lock.Lockfile;
import io.qrun.qctl.qbit.lock.LockfileIO;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "resolve", description = "Resolve qBits (V1 hermetic stub)")
public class ResolveCommand implements Runnable {
  @Option(names = "--lockfile", description = "Path to lockfile", defaultValue = "qbits.lock")
  Path lockfilePath;

  @Option(names = "--hermetic", description = "Resolve strictly from lockfile")
  boolean hermetic;

  @Override
  public void run() {
    try {
      Lockfile lf = LockfileIO.read(lockfilePath);
      if (hermetic) {
        int count = lf.packages != null ? lf.packages.size() : 0;
        System.out.println(summary(count));
      } else {
        System.out.println("non-hermetic resolution TODO (V1)");
      }
    } catch (Exception e) {
      System.err.println("qbit resolve error: " + e.getMessage());
      System.exit(1);
    }
  }

  static String summary(int count) {
    return "Would install " + count + " package(s) from lockfile";
  }
}
