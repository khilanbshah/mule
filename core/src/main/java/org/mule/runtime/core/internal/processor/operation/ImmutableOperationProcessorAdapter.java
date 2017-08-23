/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.operation;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.Operation;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * An implementation of {@link Operation} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableOperationProcessorAdapter<T, A> implements Operation<T, A>, Initialisable {

  /**
   * Processor that will be executed upon calling process
   */
  private Processor processor;

  /**
   * Event that will be cloned for dispatching
   */
  private InternalEvent event;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link InternalEvent} for the execution of the given chain
   * @param processor a {@link Processor} chain to be executed
   */
  public ImmutableOperationProcessorAdapter(InternalEvent event, Processor processor) {
    this.event = event;
    this.processor = processor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(CompletionCallback<T, A> callback) {
    doProcess(event, callback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Result previous, CompletionCallback callback) {
    InternalEvent updatedEvent = InternalEvent.builder(event).message(toMessage(previous)).build();
    doProcess(updatedEvent, callback);
  }

  private void doProcess(InternalEvent updatedEvent, CompletionCallback callback) {
    MonoProcessor<InternalEvent> subscribe = Mono.from(processor.apply(Mono.just(updatedEvent)))
        .doOnSuccess(result -> callback.success(Result.<T, A>builder(result.getMessage()).build()))
        .doOnError(callback::error)
        .subscribe();
  }

  private Message toMessage(Result previous) {
    Message.Builder messageBuilder = Message.builder().value(previous.getOutput());
    previous.getMediaType().ifPresent(mediaType -> messageBuilder.mediaType((MediaType) mediaType));
    previous.getAttributes().ifPresent(messageBuilder::attributesValue);
    previous.getAttributesMediaType().ifPresent(mediaType -> messageBuilder.attributesMediaType((MediaType) mediaType));
    return messageBuilder.build();
  }

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(processor);
  }
}
