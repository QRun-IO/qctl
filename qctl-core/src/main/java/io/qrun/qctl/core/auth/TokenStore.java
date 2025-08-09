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


/**
 * Minimal token store using a file in the configuration directory.
 *
 * Why: Provide a simple, testable storage mechanism for API tokens in V1.
 * @since 0.1.0
 */
public final class TokenStore
{
   private final Path file;



   /**
    * Creates a token store in the given config directory.
    *
    * @param configDir configuration directory where tokens are stored
    */
   public TokenStore(Path configDir)
   {
      this.file = configDir.resolve("tokens.json");
   }



   /**
    * Reads the API key if present.
    *
    * @return optional API key
    */
   public Optional<String> readApiKey()
   {
      try
      {
         if(!Files.exists(file))
         {
            return Optional.empty();
         }
         String s = Files.readString(file).trim();
         if(s.isEmpty())
         {
            return Optional.empty();
         }
         return Optional.of(s);
      }
      catch(IOException e)
      {
         return Optional.empty();
      }
   }



   /**
    * Writes the API key to disk.
    *
    * @param key API key
    */
   public void writeApiKey(String key) throws IOException
   {
      Files.createDirectories(file.getParent());
      Files.writeString(file, key);
   }



   /** Deletes the stored token file if it exists. */
   public void clear() throws IOException
   {
      Files.deleteIfExists(file);
   }
}
