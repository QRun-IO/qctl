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


import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class TokenStoreTest
{
   @Test
   void write_read_and_clear_api_key() throws Exception
   {
      Path tmp = Files.createTempDirectory("qctl-test-config");
      TokenStore store = new TokenStore(tmp);

      assertThat(store.readApiKey()).isEmpty();

      store.writeApiKey("test-key");
      assertThat(store.readApiKey()).contains("test-key");

      store.clear();
      assertThat(store.readApiKey()).isEmpty();
   }
}
