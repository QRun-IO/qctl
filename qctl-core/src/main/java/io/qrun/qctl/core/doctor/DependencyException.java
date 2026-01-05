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
 * Exception thrown when a required dependency is not satisfied.
 *
 * @since 0.2.0
 *******************************************************************************/
public class DependencyException extends Exception
{
   private final DependencyStatus status;



   /***************************************************************************
    * Create exception from dependency status.
    *
    * @param status the failing dependency status
    * @since 0.2.0
    ***************************************************************************/
   public DependencyException(DependencyStatus status)
   {
      super(status.getMessage());
      this.status = status;
   }



   /***************************************************************************
    * Get the dependency status that caused this exception.
    *
    * @return dependency status
    * @since 0.2.0
    ***************************************************************************/
   public DependencyStatus getStatus()
   {
      return status;
   }



   /***************************************************************************
    * Get a user-friendly error message with installation hint.
    *
    * @return formatted error message
    * @since 0.2.0
    ***************************************************************************/
   public String getFormattedMessage()
   {
      StringBuilder sb = new StringBuilder();
      sb.append(status.getMessage());
      if(status.hint() != null && !status.hint().isEmpty())
      {
         sb.append("\n").append(status.hint());
      }
      return sb.toString();
   }
}
