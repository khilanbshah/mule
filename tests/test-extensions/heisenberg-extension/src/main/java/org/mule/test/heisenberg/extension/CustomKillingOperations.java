/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.KILL_WITH_GROUP;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.Operation;
import org.mule.test.heisenberg.extension.model.KillParameters;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.WeaponType;
import org.mule.test.heisenberg.extension.stereotypes.KillingStereotype;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

@Stereotype(KillingStereotype.class)
public class CustomKillingOperations {

  public String killWithCustomMessage(@ParameterGroup(name = KILL_WITH_GROUP) KillParameters killParameters) {
    return format("%s, %s", killParameters.getGoodbyeMessage(), killParameters.getVictim());
  }

  public List<Ricin> killWithRicins(@Optional(defaultValue = PAYLOAD) List<Ricin> ricins) {
    return ricins;
  }

  public String killWithWeapon(Weapon weapon, WeaponType type, Weapon.WeaponAttributes attributesOfWeapon) {
    return format("Killed with: %s , Type %s and attribute %s", weapon.kill(), type.name(), attributesOfWeapon.getBrand());
  }

  public List<String> killWithMultiplesWeapons(@Optional(defaultValue = PAYLOAD) List<Weapon> weapons) {
    return weapons.stream().map(Weapon::kill).collect(Collectors.toList());
  }

  public List<String> killWithMultipleWildCardWeapons(List<? extends Weapon> wildCardWeapons) {
    return wildCardWeapons.stream().map(Weapon::kill).collect(Collectors.toList());
  }

  public int killWithId(int id) {
    return id;
  }

  @Throws(HeisenbergErrorTyperProvider.class)
  public void killMany(@AllowedStereotypes(KillingStereotype.class) Chain<Object, Object> killOperations, String reason,
                       CompletionCallback<String, Void> callback)
      throws Exception {
    // killOperations.process(new CompletionCallback() {
    //
    //   @Override
    //   public void success(Result result) {
    //     callback.success(result);
    //   }
    //
    //   @Override
    //   public void error(Throwable e) {
    //     e.printStackTrace();
    //     callback.error(e);
    //   }
    // });
    final StringBuilder sb = new StringBuilder();
    final CountDownLatch doneSignal = new CountDownLatch(killOperations.getOperations().size());

    try {
      for (Operation validation : killOperations.getOperations()) {
        validation.process(new CompletionCallback() {

          @Override
          public void success(Result result) {
            sb.append(result.getOutput());
            doneSignal.countDown();
          }

          @Override
          public void error(Throwable e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause == null) {
              rootCause = e;
            }
            sb.append(rootCause.getMessage());
            doneSignal.countDown();
          }
        });
      }

      doneSignal.await();

      callback.success(Result.<String, Void>builder().output(sb.toString()).build());

    } catch (Throwable e) {
      callback.error(e);
    }
  }

  public void executeAnything(Chain<?, ?> chain, CompletionCallback<Void, Void> callback) {
    chain.process(new NullCompetitionCallback(callback));
  }

  private class NullCompetitionCallback implements CompletionCallback {

    private CompletionCallback<?, ?> callback;

    NullCompetitionCallback(CompletionCallback<?, ?> callback) {
      this.callback = callback;
    }

    @Override
    public void success(Result result) {
      callback.success(result);
    }

    @Override
    public void error(Throwable e) {
      callback.error(e);
    }
  }
}
