package com.axonivy.loadtest.dsl;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpCookies;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpDefaults;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

import com.axonivy.loadtest.dsl.config.IvyAppConfig;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.engines.EmbeddedJmeterEngine;
import us.abstracta.jmeter.javadsl.core.threadgroups.BaseThreadGroup.ThreadGroupChild;

/**
 * Fluent entry point for running an Axon Ivy / JSF load test.
 *
 * <p>Wires the JMeter engine (HTTP defaults, cookies, browser headers, CSV dataset, JTL writer, HTML
 * reporter) around your scenario steps, runs it as a single-scenario test, and asserts zero errors.
 * Configure it with named, defaulted options instead of positional arguments:
 *
 * <pre>{@code
 * import static com.axonivy.loadtest.dsl.IvyJsfDsl.*;
 *
 * IvyLoadTestRunner.builder(APP, "my-scenario")
 *     .steps(
 *         openProcess(APP, "Open Home", HOME_PROCESS),
 *         clickButton(APP, "Click something", "form:button").assertOk().build())
 *     .run();
 * }</pre>
 *
 * <p>{@link Builder#csvData(String)}, {@link Builder#users(String)} and {@link Builder#rampUp(String)}
 * default to the standard {@code test.properties} expressions, so a typical scenario only needs to set
 * {@code .steps(...)}. Override any of them when your scenario differs.
 *
 * <p>The {@link IvyAppConfig} passed to {@link #builder(IvyAppConfig, String)} supplies the HTTP
 * host/port for the whole test; each step still takes its own config, so a single scenario can span
 * projects (e.g. a cross-project process start).
 */
public final class IvyLoadTestRunner {

  private IvyLoadTestRunner() {
  }

  /**
   * Starts a builder for a load test named {@code testName} (used for the JTL/HTML report filenames),
   * targeting the host/port in {@code app}.
   */
  public static Builder builder(IvyAppConfig app, String testName) {
    return new Builder(app, testName);
  }

  /** Fluent, named-option configuration for a single load-test run. */
  public static final class Builder {

    private final IvyAppConfig app;
    private final String testName;
    private String csvData = "${__P(one_user.csv)}";
    private String users = "${__P(load.users)}";
    private String rampUp = "${__P(load.rampup)}";
    private ThreadGroupChild[] steps = new ThreadGroupChild[0];

    private Builder(IvyAppConfig app, String testName) {
      this.app = app;
      this.testName = testName;
    }

    /**
     * Path to the credentials CSV (no header row; columns {@code username,password}). Defaults to
     * {@code "${__P(one_user.csv)}"}.
     */
    public Builder csvData(String csvFilePath) {
      this.csvData = csvFilePath;
      return this;
    }

    /** Virtual users to ramp to, as a JMeter expression. Defaults to {@code "${__P(load.users)}"}. */
    public Builder users(String numberOfUsers) {
      this.users = numberOfUsers;
      return this;
    }

    /** Virtual users to ramp to, as a literal count. */
    public Builder users(int numberOfUsers) {
      this.users = String.valueOf(numberOfUsers);
      return this;
    }

    /** Ramp-up in seconds, as a JMeter expression. Defaults to {@code "${__P(load.rampup)}"}. */
    public Builder rampUp(String rampUpPeriod) {
      this.rampUp = rampUpPeriod;
      return this;
    }

    /** Ramp-up in seconds, as a literal count. */
    public Builder rampUp(int rampUpPeriod) {
      this.rampUp = String.valueOf(rampUpPeriod);
      return this;
    }

    /** The scenario steps, in order — composable jmeter-dsl elements from {@link IvyJsfDsl}. */
    public Builder steps(ThreadGroupChild... steps) {
      this.steps = steps;
      return this;
    }

    /** Wires the engine around the configured steps, runs the test, and asserts zero errors. */
    public void run() throws IOException, InterruptedException, TimeoutException {
      IvyLoadTestRunner.execute(app, testName, csvData, users, rampUp, steps);
    }
  }

  private static void execute(IvyAppConfig session, String testName, String csvFilePath,
      String numberOfUsers, String rampUpPeriod, ThreadGroupChild... steps)
      throws IOException, InterruptedException, TimeoutException {

    ThreadGroupChild[] children = new ThreadGroupChild[steps.length + 4];
    // Build via url() rather than host()/port(): jmeter-java-dsl's port() only accepts an int,
    // whereas url() stores host/port verbatim as strings, so JMeter expressions such as
    // "${__P(server.port)}" survive and are resolved at runtime from test.properties.
    children[0] = httpDefaults().url("http://" + session.host() + ":" + session.port());
    children[1] = httpCookies();
    children[2] = httpHeaders()
        .header("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Connection", "keep-alive")
        .header("Upgrade-Insecure-Requests", "1")
        .header("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36");
    children[3] = csvDataSet(csvFilePath)
        .variableNames("username,password")
        .delimiter(",")
        .ignoreFirstLine(false);
    System.arraycopy(steps, 0, children, 4, steps.length);

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String jtlDir = String.format("target/jtls/%s", timestamp);
    TestPlanStats stats = testPlan(
        threadGroup(testName)
            .rampTo(numberOfUsers, rampUpPeriod)
            .holdIterating(1)
            .children(children),
        // Uncomment for local debugging:
        // resultsTreeVisualizer(),
        jtlWriter(jtlDir, testName + ".jtl"),
        htmlReporter("target/html-report/" + testName))
        .runIn(new EmbeddedJmeterEngine().propertiesFile("resources/test.properties"));

    assertNoErrors(stats, testName);
  }

  private static void assertNoErrors(TestPlanStats stats, String testName) {
    long errors = stats.overall().errorsCount();
    long samples = stats.overall().samplesCount();
    if (errors > 0) {
      throw new AssertionError(String.format(
          "%s failed: %d errors out of %d samples (%.2f%% error rate)",
          testName, errors, samples, (double) errors / samples * 100));
    }
    System.out.printf("%s completed successfully: %d samples, 0 errors%n", testName, samples);
  }
}
