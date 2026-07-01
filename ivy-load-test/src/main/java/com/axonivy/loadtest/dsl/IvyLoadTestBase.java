package com.axonivy.loadtest.dsl;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.axonivy.loadtest.dsl.config.IvyAppConfig;

import us.abstracta.jmeter.javadsl.core.threadgroups.BaseThreadGroup.ThreadGroupChild;

/**
 * Base class for Axon Ivy / JSF load tests, retained for back-compat and convenience.
 *
 * <p>New tests should prefer {@link IvyLoadTestRunner#builder(IvyAppConfig, String)} for its named,
 * defaulted options — no need to extend this class. These {@code run(...)} overloads remain as a thin
 * delegate to that runner so existing tests keep working unchanged:
 *
 * <pre>{@code
 * import static com.axonivy.loadtest.dsl.IvyJsfDsl.*;
 *
 * public class MyLoadTest {
 *   private static final IvyAppConfig APP = IvyAppConfig.builder()...build();
 *
 *   &#64;Test
 *   public void myScenario() throws Exception {
 *     IvyLoadTestRunner.builder(APP, "my-scenario")
 *         .steps(openProcess(APP, "Home", "MyProcess.ivp"))
 *         .run();
 *   }
 * }
 * }</pre>
 *
 * <p>Step builders ({@code openProcess}, {@code jsfAjax}, {@code openRedirect}, …) live in the
 * stateless {@link IvyJsfDsl} facade and take the {@link IvyAppConfig} explicitly.
 */
public abstract class IvyLoadTestBase {

  /**
   * Convenience overload taking a literal {@code int} user count and ramp-up (seconds).
   */
  protected void run(IvyAppConfig session, String testName, String csvFilePath,
      int numberOfUsers, int rampUpPeriod, ThreadGroupChild... steps)
      throws IOException, InterruptedException, TimeoutException {
    run(session, testName, csvFilePath,
        String.valueOf(numberOfUsers), String.valueOf(rampUpPeriod), steps);
  }

  /**
   * Runs the given steps as a single-scenario load test and asserts zero errors.
   *
   * <p>Delegates to {@link IvyLoadTestRunner}; prefer that builder directly in new tests.
   *
   * @param session       app coordinates used for HTTP defaults
   * @param testName      label used for JTL/HTML report filenames
   * @param csvFilePath   path to the credentials CSV (no header row; columns: username,password)
   * @param numberOfUsers virtual users to ramp to (a number or JMeter expression, e.g. "${__P(load.users)}")
   * @param rampUpPeriod  ramp-up in seconds (a number or JMeter expression, e.g. "${__P(load.rampup)}")
   * @param steps         scenario steps — composable jmeter-dsl elements
   */
  protected void run(IvyAppConfig session, String testName, String csvFilePath,
      String numberOfUsers, String rampUpPeriod, ThreadGroupChild... steps)
      throws IOException, InterruptedException, TimeoutException {
    IvyLoadTestRunner.builder(session, testName)
        .csvData(csvFilePath)
        .users(numberOfUsers)
        .rampUp(rampUpPeriod)
        .steps(steps)
        .run();
  }
}
