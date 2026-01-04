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

package io.qrun.qctl.qqq.template;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*******************************************************************************
 * Transform definition for post-render file operations.
 *
 * Supports two transform types:
 * - rename: Rename files matching a glob pattern
 * - delete: Delete files matching a glob pattern
 *
 * @param type transform type (rename or delete)
 * @param pattern glob pattern to match files (e.g., "**\/*.java")
 * @param replacement replacement pattern for rename (uses $varName substitution)
 * @since 0.2.0
 *******************************************************************************/
@JsonIgnoreProperties(ignoreUnknown = true)
public record Transform(
   String type,
   String pattern,
   String replacement
)
{
   /***************************************************************************
    * Transform type constants.
    ***************************************************************************/
   public static final String TYPE_RENAME = "rename";
   public static final String TYPE_DELETE = "delete";



   /***************************************************************************
    * Validate this transform definition.
    *
    * @throws IllegalArgumentException if transform is invalid
    * @since 0.2.0
    ***************************************************************************/
   public void validate()
   {
      if(type == null || type.isBlank())
      {
         throw new IllegalArgumentException("Transform type is required");
      }

      if(!TYPE_RENAME.equals(type) && !TYPE_DELETE.equals(type))
      {
         throw new IllegalArgumentException("Invalid transform type: " + type + ". Must be 'rename' or 'delete'");
      }

      if(pattern == null || pattern.isBlank())
      {
         throw new IllegalArgumentException("Transform pattern is required");
      }

      if(TYPE_RENAME.equals(type) && (replacement == null || replacement.isBlank()))
      {
         throw new IllegalArgumentException("Replacement is required for rename transforms");
      }
   }
}
