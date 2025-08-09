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

package io.qrun.qctl.qrun;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import io.qrun.qctl.core.output.Output;
import picocli.CommandLine.Command;


@Command(name = "package", description = "Prepare dummy artifact manifest (mock V1)")
public class PackageCommand implements Runnable
{
   @Override
   public void run()
   {
      try
      {
         Path target = Path.of("target");
         Files.createDirectories(target);
         Path                manifest = target.resolve("artifact-manifest.json");
         Map<String, Object> m        = new LinkedHashMap<>();
         m.put("kind", "oci");
         m.put("digest", "sha256:d34db33f");
         m.put("sizeBytes", 12345678);
         m.put("createdAt", Instant.now().toString());
         String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
         Files.writeString(manifest, json);
         Output.text(System.out, "wrote " + manifest);
      }
      catch(IOException e)
      {
         System.err.println("qrun package error: " + e.getMessage());
         System.exit(1);
      }
   }
}
