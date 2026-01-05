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

package io.qrun.qctl.qqq.registry;


import java.util.List;
import io.qrun.qctl.qqq.template.ComputedVariable;
import io.qrun.qctl.qqq.template.TemplateManifest;
import io.qrun.qctl.qqq.template.Transform;


/*******************************************************************************
 * Template metadata from the registry.
 *
 * Contains all information needed to display and initialize a template.
 *
 * @param id unique template identifier
 * @param name display name
 * @param version template version (semver)
 * @param description brief description
 * @param downloadUrl URL to fetch template archive or git repo
 * @param schemaVersion manifest schema version
 * @param minimumQctlVersion minimum qctl version required
 * @param tags categorization tags
 * @param maintainer template maintainer
 * @param prompts interactive prompts for template variables
 * @param computed computed variables derived from prompts
 * @param transforms file transforms (rename/delete)
 * @param postGen post-generation hooks
 * @param ignore patterns to ignore during rendering
 * @since 0.2.0
 *******************************************************************************/
public record TemplateInfo(
   String id,
   String name,
   String version,
   String description,
   String downloadUrl,
   Integer schemaVersion,
   String minimumQctlVersion,
   List<String> tags,
   String maintainer,
   List<TemplateManifest.Prompt> prompts,
   List<ComputedVariable> computed,
   List<Transform> transforms,
   List<TemplateManifest.PostGenHook> postGen,
   List<String> ignore
)
{
   /***************************************************************************
    * Convert to TemplateManifest for rendering.
    *
    * @return template manifest
    * @since 0.2.0
    ***************************************************************************/
   public TemplateManifest toManifest()
   {
      return new TemplateManifest(
         schemaVersion,
         id,
         name,
         version,
         description,
         minimumQctlVersion,
         prompts,
         computed,
         transforms,
         postGen,
         ignore
      );
   }
}
