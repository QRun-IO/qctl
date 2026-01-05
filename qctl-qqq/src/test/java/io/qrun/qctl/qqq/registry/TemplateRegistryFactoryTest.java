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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for TemplateRegistryFactory.
 *
 * @since 0.2.0
 *******************************************************************************/
class TemplateRegistryFactoryTest
{
   @BeforeEach
   void setUp()
   {
      TemplateRegistryFactory.reset();
   }



   @AfterEach
   void tearDown()
   {
      TemplateRegistryFactory.reset();
   }



   @Test
   void testGetRegistry_returnsSameInstance()
   {
      TemplateRegistry first = TemplateRegistryFactory.getRegistry();
      TemplateRegistry second = TemplateRegistryFactory.getRegistry();

      assertThat(first).isSameAs(second);
   }



   @Test
   void testReset_clearsCache()
   {
      TemplateRegistry first = TemplateRegistryFactory.getRegistry();
      TemplateRegistryFactory.reset();
      TemplateRegistry second = TemplateRegistryFactory.getRegistry();

      assertThat(first).isNotSameAs(second);
   }



   @Test
   void testSetRegistry_overridesDefault()
   {
      MockTemplateRegistry mock = new MockTemplateRegistry();
      TemplateRegistryFactory.setRegistry(mock);

      TemplateRegistry result = TemplateRegistryFactory.getRegistry();

      assertThat(result).isSameAs(mock);
   }
}
