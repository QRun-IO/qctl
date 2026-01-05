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

package io.qrun.qctl.shared;


/*******************************************************************************
 * Standard exit codes for qctl commands.
 *
 * Why: Consistent exit codes enable scripting and CI/CD integration. These
 * codes map directly to RFC 7807 problem types and HTTP status categories.
 *
 * @since 0.1.0
 *******************************************************************************/
public final class ExitCodes
{
   /** Operation completed successfully. */
   public static final int SUCCESS = 0;

   /** Generic/unexpected error. */
   public static final int GENERIC = 1;

   /** Usage or configuration error (bad arguments, invalid config). */
   public static final int USAGE = 2;

   /** Network error (connection failed, timeout, 408/429/5xx). */
   public static final int NETWORK = 3;

   /** Authentication error (401/403 from API). */
   public static final int AUTH = 4;

   /** Resource not found (404 from API). */
   public static final int NOT_FOUND = 5;

   /** Validation error (400/422, bad input). */
   public static final int VALIDATION = 6;

   /** Integrity error (hash/signature mismatch). */
   public static final int INTEGRITY = 7;

   /** Conflict error (409, state conflict). */
   public static final int CONFLICT = 8;

   /** Cancelled by user (Ctrl+C, SIGINT). */
   public static final int CANCELLED = 9;

   /***************************************************************************
    * Non-instantiable utility class.
    ***************************************************************************/
   private ExitCodes()
   {
   }
}
