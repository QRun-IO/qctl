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


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import io.qrun.qctl.core.auth.TokenStore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class AuthCommandsTest
{
   @Test
   void login_with_api_key_writes_store() throws Exception
   {
     Path tmp = Files.createTempDirectory("qctl-auth");

     class TestLogin extends LoginCommand
     {
       @Override
       protected TokenStore createStore()
       {
         return new TokenStore(tmp);
       }
     }

     ByteArrayOutputStream out = new ByteArrayOutputStream();
     PrintStream stdout = new PrintStream(out);
     PrintStream orig = System.out;
     try
     {
       System.setOut(stdout);
       TestLogin cmd = new TestLogin();
       cmd.apiKey = "abc";
       cmd.run();
       String s = out.toString();
       assertThat(s).contains("API key stored");
       assertThat(new TokenStore(tmp).readApiKey()).contains("abc");
     }
     finally
     {
       System.setOut(orig);
     }
   }

   @Test
   void logout_clears_store() throws Exception
   {
     Path tmp = Files.createTempDirectory("qctl-auth");
     TokenStore store = new TokenStore(tmp);
     store.writeApiKey("abc");

     class TestLogout extends LogoutCommand
     {
       @Override
       protected TokenStore createStore()
       {
         return new TokenStore(tmp);
       }
     }

     ByteArrayOutputStream out = new ByteArrayOutputStream();
     PrintStream stdout = new PrintStream(out);
     PrintStream orig = System.out;
     try
     {
       System.setOut(stdout);
       new TestLogout().run();
       String s = out.toString();
       assertThat(s).contains("removed stored token");
       assertThat(store.readApiKey()).isEmpty();
     }
     finally
     {
       System.setOut(orig);
     }
   }
}
