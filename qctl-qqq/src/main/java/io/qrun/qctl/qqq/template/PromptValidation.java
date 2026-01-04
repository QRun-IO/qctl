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


import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/*******************************************************************************
 * Validation rules for template prompts.
 *
 * Supports string validation (pattern, length) and integer validation (range).
 *
 * @param pattern regex pattern for string validation
 * @param minLength minimum string length
 * @param maxLength maximum string length
 * @param min minimum value for integers
 * @param max maximum value for integers
 * @param values allowed values (enum validation)
 * @param message custom error message
 * @since 0.2.0
 *******************************************************************************/
@JsonIgnoreProperties(ignoreUnknown = true)
public record PromptValidation(
   String pattern,
   Integer minLength,
   Integer maxLength,
   Integer min,
   Integer max,
   List<String> values,
   String message
)
{
}
