/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.chain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.junit.Test;

public class ChainExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "chain/heisenberg-chain.xml";
  }

  @Test
  public void executeManyKillOperations() throws Exception {
    InternalEvent result = flowRunner("executeManyKillOperations").run();
    assertThat(result.getMessage().getPayload().getValue(), is(3));
  }

  @Test
  public void voidConstructDoesNotMutateMessage() throws Exception {
    InternalEvent result = flowRunner("voidConstructDoesNotMutateMessage").run();
    assertThat(result.getMessage().getPayload().getValue(), is("original payload"));
  }

  @Test
  public void executeAnyAvailableComponent() throws Exception {
    InternalEvent anything = flowRunner("execute-anything").run();
    System.out.println(anything.getMessage().getPayload().getValue());
  }
}
