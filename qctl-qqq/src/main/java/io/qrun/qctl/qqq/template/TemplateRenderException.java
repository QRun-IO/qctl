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


/*******************************************************************************
 * Exception thrown when template rendering fails.
 *
 * Provides context about the template error including location information
 * when available from the underlying template engine.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TemplateRenderException extends Exception
{


   /***************************************************************************
    * Create a new exception with a message.
    *
    * @param message error message
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message)
   {
      super(message);
   }



   /***************************************************************************
    * Create a new exception with a message and cause.
    *
    * @param message error message
    * @param cause underlying cause
    * @since 0.2.0
    ***************************************************************************/
   public TemplateRenderException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
