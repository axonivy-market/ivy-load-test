package com.axonivy.loadtest.dsl.jsf;

import static us.abstracta.jmeter.javadsl.JmeterDsl.responseAssertion;

import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion;
import us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;

/** Reusable response assertions for JSF/Ivy load tests. */
public final class JsfAssertions {

  private JsfAssertions() {
  }

  /** Asserts the HTTP response code is {@code 200}. */
  public static DslResponseAssertion ok() {
    return responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200");
  }
}
