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
import picocli.CommandLine.Command;


@Command(name = "logout", description = "Remove stored credentials (V1)")
public class LogoutCommand implements Runnable
{
   @Override
   public void run()
   {
      try
      {
         createStore().clear();
         System.out.println("auth logout: removed stored token");
      }
      catch(Exception e)
      {
         System.err.println("auth logout error: " + e.getMessage());
         System.exit(1);
      }
   }



   /***************************************************************************
    * Creates a TokenStore. Overridable in tests to inject a temporary store.
    *
    * @return token store instance rooted at the user config directory
    ***************************************************************************/
   protected TokenStore createStore()
   {
      return new TokenStore(SystemPaths.configDir());
   }
}
