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

package io.qrun.qctl.shared.api;


import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail
{
   /***************************************************************************
    * RFC 7807 Problem Details structure used for API errors.
    *
    * Why: Provide a consistent, typed error envelope across endpoints and clients.
    * @since 0.1.0
    ***************************************************************************/
   public String   type;
   public String   title;
   public Integer  status;
   public String   detail;
   public String   instance;
   public String[] errors;
}
