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


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


/*******************************************************************************
 * Registry for fetching and managing project templates.
 *
 * Provides abstraction over template sources (GitHub, Voyage API, mock).
 * Implementations handle caching, authentication, and download logic.
 *
 * @since 0.2.0
 *******************************************************************************/
public interface TemplateRegistry
{
   /***************************************************************************
    * List all available templates.
    *
    * @return list of template metadata
    * @throws IOException if registry cannot be reached
    * @since 0.2.0
    ***************************************************************************/
   List<TemplateInfo> listTemplates() throws IOException;



   /***************************************************************************
    * Get template by ID.
    *
    * @param id template identifier
    * @return template metadata if found
    * @throws IOException if registry cannot be reached
    * @since 0.2.0
    ***************************************************************************/
   Optional<TemplateInfo> getTemplate(String id) throws IOException;



   /***************************************************************************
    * Download template to local path.
    *
    * @param id template identifier
    * @param version specific version or null for latest
    * @return path to downloaded template directory
    * @throws IOException if download fails
    * @since 0.2.0
    ***************************************************************************/
   Path downloadTemplate(String id, String version) throws IOException;



   /***************************************************************************
    * Refresh the template cache.
    *
    * Forces a fresh fetch from the registry on next call.
    *
    * @since 0.2.0
    ***************************************************************************/
   void refresh();
}
