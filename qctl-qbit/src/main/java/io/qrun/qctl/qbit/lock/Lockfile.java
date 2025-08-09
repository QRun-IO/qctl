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

package io.qrun.qctl.qbit.lock;


import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lockfile
{
   /***************************************************************************
    * qBit lockfile v1 model used to capture resolved dependencies deterministically.
    *
    * Why: Provides a hermetic snapshot of qBits with resolved URLs and integrity hashes.
    * @since 0.1.0
    ***************************************************************************/
   public int                       lockfileVersion = 1;
   public String                    generatedAt     = Instant.now().toString();
   public Map<String, PackageEntry> packages        = new LinkedHashMap<>();



   @JsonInclude(JsonInclude.Include.NON_NULL)
   public static class PackageEntry
   {
      /***************************************************************************
       * Entry describing a single resolved qBit artifact in the lockfile.
       *
       * Why: Encapsulates resolved URL, integrity checksum, and dependency edges.
       * @since 0.1.0
       ***************************************************************************/
      public String              name;
      public String              version;
      public String              resolved;
      public String              integrity;
      public Map<String, String> dependencies;
      public String              registry;
      public String              vendorPath;
      public String              signature;
   }
}
