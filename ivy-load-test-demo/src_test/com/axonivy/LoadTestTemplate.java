package com.axonivy;

import static com.axonivy.loadtest.dsl.IvyJsfDsl.*;

import com.axonivy.loadtest.dsl.IvyLoadTestRunner;
import com.axonivy.loadtest.dsl.config.IvyAppConfig;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

/**
 * Starter template — copy this class, point it at your app, and add your steps.
 *
 * <p><b>What you do:</b>
 * <ol>
 *   <li>Set your server in {@code resources/test.properties} (host, port, application, project).</li>
 *   <li>Set the {@link #APP} coordinates and {@link #HOME_PROCESS} below to match your app.</li>
 *   <li>If your app needs a login, add a {@code username,password} line to
 *       {@code resources/one_user.csv}.</li>
 *   <li>Describe your scenario in {@link #testMyScenario()} — the full menu of step builders is
 *       shown commented in that method; uncomment and adapt the ones you need.</li>
 *   <li>Run it: {@code mvn -pl ivy-load-test-demo test -Dtest=LoadTestTemplate -Plocal-portal}</li>
 * </ol>
 *
 * <p>The {@link com.axonivy.loadtest.dsl.IvyJsfDsl} facade hides every JSF/PrimeFaces wire detail
 * (ViewState, AJAX params, redirects) — see its Javadoc for the full API. For a complete, realistic
 * scenario to learn from, see {@link PortalWalkthroughLoadTest}.
 */
public class LoadTestTemplate {

  // 1) Your app coordinates — most values come from resources/test.properties.
  private static final IvyAppConfig APP = IvyAppConfig.builder()
      .host("${__P(server.host)}")
      .port("${__P(server.port)}")
      .securitySystem("${__P(security.system.name)}")
      .application("${__P(application.name)}")
      .project("${__P(project.name)}")
      .processHash("1549F58C18A6C562")   // the hash in your .ivp URL (visible in Designer)
      .build();

  // 2) The process opened first — your app's entry / home page.
  private static final String HOME_PROCESS = "DefaultApplicationHomePage.ivp";

  @Test
  public void testMyScenario() throws IOException, InterruptedException, TimeoutException {
    // csvData / users / rampUp default to the standard test.properties expressions
    // (${__P(one_user.csv)}, ${__P(load.users)}, ${__P(load.rampup)}). Chain e.g.
    // .users("50").rampUp("30").csvData("${__P(many_users.csv)}") before .steps(...) to override.
    IvyLoadTestRunner.builder(APP, "my-scenario")
        .steps(

            // Start here — open your entry page:
            openProcess(APP, "Open Home", HOME_PROCESS)

            // Then add steps below (drop the leading "//" to enable one). The building blocks —
            // see IvyJsfDsl for full docs and PortalWalkthroughLoadTest for them used in anger:
            //
            //   // log in (reads ${username} / ${password} from one_user.csv):
            //   , login(APP, "login:login-form", "login-form:login-command")
            //         .field("login:login-form:username", "${username}")
            //         .field("login:login-form:password", "${password}")
            //         .assertOk().build()
            //
            //   // a widget that auto-loads after the page renders:
            //   , autoLoad(APP, "Load widget", "widget:rcLoadFirstTime")
            //         .updates("widget:container").assertOk().build()
            //
            //   // a button / link click that re-renders part of the page:
            //   , clickButton(APP, "Click something", "form:button")
            //         .updates("form:panel").assertOk().build()
            //
            //   // a single search / filter input posting its own value:
            //   , submitField(APP, "Search", "search-form", "search-form:input", "keyword")
            //         .updates("results").assertOk().build()
            //
            //   // navigate via a redirect-emitting click, then follow the redirect:
            //   , clickButton(APP, "Open section", "nav:menu").param("nav:menu_menuid", "x")
            //         .expectRedirect().assertOk().build()
            //   , openRedirect(APP, "Opened section")
        )
        .run();
  }
}
