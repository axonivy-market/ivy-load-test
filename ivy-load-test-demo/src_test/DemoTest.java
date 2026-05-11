package com.axonivy;

import us.abstracta.jmeter.javadsl.core.engines.EmbeddedJmeterEngine;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static us.abstracta.jmeter.javadsl.JmeterDsl.*;
import static us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.*;

public class DemoTest {

  private String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

  @Test
  public void testAxonIvyWebsite() throws IOException, InterruptedException, TimeoutException {
    String jtlDirName = String.format("target/jtls/%s", timestamp);
    String testName = "axonivy-website-test";
    
    testPlan(
      threadGroup("Test Axon Ivy Website")
        .rampTo(1, Duration.ofSeconds(1))
        .holdIterating(1)
        .children(
            httpHeaders().header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36"),

            httpSampler("Go to Axon Ivy website" ,"https://www.axonivy.com/platform")
            .method("GET")
            .children(
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200"),
              responseAssertion().containsSubstrings("Axon Ivy Automation Platform")
            ),

            httpSampler("Go to Axon Ivy Market" ,"https://market.axonivy.com/")
            .method("GET")
            .children(
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200"),
              responseAssertion().containsSubstrings("Axon Ivy Market"),
              responseAssertion().containsSubstrings("The Axon Ivy market offers a unique experience to accelerate process automation.")
            )
          ),
      // Remove comment the line below on local to debug
      // resultsTreeVisualizer(),

      // Listeners and writers:
      jtlWriter(jtlDirName, testName + ".jtl"),
      htmlReporter("target/html-report/" + testName)
    ).runIn(new EmbeddedJmeterEngine());
  }
}