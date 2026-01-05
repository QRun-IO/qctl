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

package io.qrun.qctl.core.doctor;


/*******************************************************************************
 * Status of a dependency check.
 *
 * @since 0.2.0
 *******************************************************************************/
public record DependencyStatus(
   String name,
   Status status,
   String version,
   String path,
   String requiredVersion,
   String hint
)
{

   /***************************************************************************
    * Dependency check status values.
    *
    * @since 0.2.0
    ***************************************************************************/
   public enum Status
   {
      OK,
      NOT_FOUND,
      VERSION_MISMATCH,
      OPTIONAL_MISSING,
      ERROR
   }



   /***************************************************************************
    * Check if dependency is satisfied.
    *
    * @return true if OK
    * @since 0.2.0
    ***************************************************************************/
   public boolean isOk()
   {
      return status == Status.OK;
   }



   /***************************************************************************
    * Check if this is an optional dependency.
    *
    * @return true if optional
    * @since 0.2.0
    ***************************************************************************/
   public boolean isOptional()
   {
      return status == Status.OPTIONAL_MISSING;
   }



   /***************************************************************************
    * Create status for a found and valid dependency.
    *
    * @param name dependency name
    * @param version found version
    * @param path executable path
    * @return OK status
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus ok(String name, String version, String path)
   {
      return new DependencyStatus(name, Status.OK, version, path, null, null);
   }



   /***************************************************************************
    * Create status for a missing required dependency.
    *
    * @param name dependency name
    * @param executable executable name
    * @param hint installation hint
    * @return NOT_FOUND status
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus notFound(String name, String executable, String hint)
   {
      return new DependencyStatus(name, Status.NOT_FOUND, null, null, null, hint);
   }



   /***************************************************************************
    * Create status for a version mismatch.
    *
    * @param name dependency name
    * @param foundVersion version that was found
    * @param requiredVersion version that is required
    * @param path executable path
    * @param hint upgrade hint
    * @return VERSION_MISMATCH status
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus versionMismatch(String name, String foundVersion,
         String requiredVersion, String path, String hint)
   {
      return new DependencyStatus(name, Status.VERSION_MISMATCH, foundVersion, path, requiredVersion, hint);
   }



   /***************************************************************************
    * Create status for a missing optional dependency.
    *
    * @param name dependency name
    * @param executable executable name
    * @param hint installation hint
    * @return OPTIONAL_MISSING status
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus optional(String name, String executable, String hint)
   {
      return new DependencyStatus(name, Status.OPTIONAL_MISSING, null, null, null, hint);
   }



   /***************************************************************************
    * Create status for an error during check.
    *
    * @param name dependency name
    * @param errorMessage error message
    * @return ERROR status
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus error(String name, String errorMessage)
   {
      return new DependencyStatus(name, Status.ERROR, null, null, null, errorMessage);
   }



   /***************************************************************************
    * Get a user-friendly message for this status.
    *
    * @return status message
    * @since 0.2.0
    ***************************************************************************/
   public String getMessage()
   {
      return switch(status)
      {
         case OK -> name + " " + version;
         case NOT_FOUND -> name + " not found";
         case VERSION_MISMATCH -> name + " " + version + " found, but " + requiredVersion + " required";
         case OPTIONAL_MISSING -> name + " not found (optional)";
         case ERROR -> name + " check failed: " + hint;
      };
   }
}
