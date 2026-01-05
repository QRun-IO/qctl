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
 * A computed variable definition for template manifests.
 *
 * Computed variables are derived from other variables using Velocity expressions.
 * They are evaluated in order after prompts are collected.
 *
 * @param name variable name to create
 * @param expression Velocity expression to evaluate
 * @since 0.2.0
 *******************************************************************************/
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComputedVariable(
   String name,
   String expression
)
{
}
