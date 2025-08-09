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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public final class LockfileIO
{
   private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());



   /***************************************************************************
    * Non-instantiable utility.
    *
    * @since 0.1.0
    ***************************************************************************/
   private LockfileIO()
   {
   }



   /***************************************************************************
    * Reads a lockfile from disk or returns an empty model if absent.
    *
    * @param path file path to read
    * @return parsed lockfile (never null)
    * @since 0.1.0
    ***************************************************************************/
   public static Lockfile read(Path path) throws IOException
   {
      if(!Files.exists(path))
      {
         return new Lockfile();
      }

      return YAML.readValue(path.toFile(), Lockfile.class);
   }



   /***************************************************************************
    * Writes a lockfile atomically (tmp + move) to avoid partial writes.
    *
    * @param path target path
    * @param lf   lockfile to serialize
    * @since 0.1.0
    ***************************************************************************/
   public static void writeAtomic(Path path, Lockfile lf) throws IOException
   {
      Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
      YAML.writeValue(tmp.toFile(), lf);
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
   }
}
