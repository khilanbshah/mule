/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.operation.ImmutableOperationProcessorAdapter;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.runtime.process.Operation;

import java.util.List;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableProcessorChainExecutor<T, A> extends ImmutableOperationProcessorAdapter<T, A>
    implements Chain<T, A>, Initialisable {

  /**
   * Adapted view of the components of this {@link Chain}
   */
  private List<Operation> operations;

  /**
   * Creates a new immutable instance
   *
   * @param event     the original {@link InternalEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChainExecutor(InternalEvent event,
                                         MessageProcessorChain chain) {
    super(event, chain);
    this.operations = chain.getMessageProcessors().stream()
        .map(processor -> new ImmutableOperationProcessorAdapter<T, A>(event, processor))
        .collect(toImmutableList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Operation> getOperations() {
    return operations;
  }

  @Override
  public void initialise() throws InitialisationException {
    for (Operation operation : operations) {
      LifecycleUtils.initialiseIfNeeded(operation);
    }
  }
}
