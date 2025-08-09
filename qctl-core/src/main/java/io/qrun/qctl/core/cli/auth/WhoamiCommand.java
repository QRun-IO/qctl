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
