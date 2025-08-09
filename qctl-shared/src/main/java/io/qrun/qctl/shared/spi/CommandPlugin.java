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

package io.qrun.qctl.shared.spi;


/**
 * Service-provider interface for dynamically contributed Picocli subcommands.
 */
public interface CommandPlugin
{
   /**
    * Returns an instance of a Picocli @Command-annotated class to register as a subcommand. The
    * returned object should be thread-safe or stateless.
    *
    * @return command instance to register
    */
   Object getCommand();
}
