package com.axonivy;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static com.axonivy.loadtest.dsl.IvyJsfDsl.autoLoad;
import static com.axonivy.loadtest.dsl.IvyJsfDsl.clickButton;
import static com.axonivy.loadtest.dsl.IvyJsfDsl.openProcess;
import static com.axonivy.loadtest.dsl.IvyJsfDsl.openRedirect;
import static com.axonivy.loadtest.dsl.IvyJsfDsl.submitField;
import com.axonivy.loadtest.dsl.IvyLoadTestRunner;
import com.axonivy.loadtest.dsl.config.IvyAppConfig;

import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * A complete, realistic Portal walkthrough — the worked example for the {@code ivy-load-test} DSL.
 * It opens the home dashboard, lets the widgets load, navigates the Processes page, starts a
 * process, quick-searches the task list, and visits the task and case dashboards.
 *
 * <p>To start your own test from a minimal skeleton, copy {@link LoadTestTemplate} instead; this
 * class is here to show what a full journey looks like end-to-end.
 *
 * <p>To adapt it to your Axon Ivy app:
 * <ol>
 *   <li>Edit {@code resources/test.properties} — host, port, app/project names, credentials CSV.</li>
 *   <li>Edit the "CUSTOMIZE FOR YOUR APP" block below — process name, JSF component ids, and the
 *       menu entries your app exposes (the {@link AppMenu} enum).</li>
 *   <li>Add/remove steps in {@link #testPortalWalkthrough()} — each step is a one-liner.</li>
 * </ol>
 *
 * <p>You should never need to edit the engine plumbing — {@link IvyLoadTestRunner} handles the JMeter
 * wiring. The {@code ivy-load-test} library handles the JSF/PrimeFaces wire format
 * (ViewState, form-action, AJAX params, redirect follow-up).
 */
public class PortalWalkthroughLoadTest {

  // ============================================================================
  // CUSTOMIZE FOR YOUR APP — only this block should change per project.
  // ============================================================================

  // 1) Server / app coordinates. Most values come from test.properties.
  private static final IvyAppConfig APP = IvyAppConfig.builder()
      .host("${__P(server.host)}")
      .port("${__P(server.port)}")
      .securitySystem("${__P(security.system.name)}")
      .application("${__P(application.name)}")
      .project("${__P(project.name)}")
      .processHash("1549F58C18A6C562")  // your Ivy process hash
      .build();

  // App config for portal-developer-examples — CategoriedLeaveRequest lives in this project.
  private static final IvyAppConfig CATEGORIED_LEAVE_REQUEST_PROCESS = APP.toBuilder()
      .project("portal-developer-examples")
      .processHash("162511D2577DBA88")
      .build();

  // 2) Entry process (.ivp file) opened at start and after login.
  private static final String HOME_PROCESS = "DefaultApplicationHomePage.ivp";

  // 3) JSF component id for the main menu — drives menuClick() below.
  private static final String MAIN_MENU = "user-menu-required-login:main-navigator:main-menu";

  // 4) Main-menu entries to navigate to. Add or remove rows for your app.
  //    Each row holds the two values that change between menu clicks:
  //      kind   -> the "menuKind" form parameter
  //      menuId -> the "<MAIN_MENU>_menuid" form parameter
  private enum AppMenu {
    TASK_LIST ("main_dashboard", "_js__default-task-list-dashboard-main-dashboard"),
    CASE_LIST ("main_dashboard", "_js__default-case-list-dashboard-main-dashboard");

    final String kind;
    final String menuId;

    AppMenu(String kind, String menuId) {
      this.kind = kind;
      this.menuId = menuId;
    }
  }

  // ============================================================================
  // SCENARIO — reads as the user journey. Edit freely to model your scenario.
  // ============================================================================

  @Test
  public void testPortalWalkthrough() throws IOException, InterruptedException, TimeoutException {
    // users / rampUp / csvData are shown explicitly here; all three default to these same
    // expressions, so a minimal scenario can omit them entirely (see LoadTestTemplate).
    IvyLoadTestRunner.builder(APP, "1_admin_user")
        .csvData("${__P(one_user.csv)}")
        .users("${__P(load.users)}")
        .rampUp("${__P(load.rampup)}")
        .steps(

            // Designer auto-auth means no login POST was captured; uncomment for a real Portal.
            openProcess(APP, "Open Home", HOME_PROCESS),
            // login(APP, "login:login-form", "login-form:login-command")
            //     .field("login:login-form:username", "${username}")
            //     .field("login:login-form:password", "${password}")
            //     .assertOk().build(),

            autoLoad(APP, "Load task widget", "task-task_1:task-component:rcLoadTaskFirstTime")
                .updates("task-task_1:task-component:dashboard-tasks-container").assertOk().build(),
            autoLoad(APP, "Load case widget", "case-case_1:case-component:rcLoadCaseFirstTime")
                .updates("case-case_1:case-component:dashboard-cases-container").assertOk().build(),
            autoLoad(APP, "Load process widget", "process-process_1:process-component:rcLoadProcessFirstTime")
                .updates("process-process_1:process-component:dashboard-processes-container").assertOk().build(),

            // Cross-project start — CategoriedLeaveRequest lives in portal-developer-examples, so a
            // separate IvyAppConfig with its own project + hash is needed.
            openProcess(CATEGORIED_LEAVE_REQUEST_PROCESS, "Start CategoriedLeaveRequest", "CategoriedLeaveRequest.ivp"),
            openProcess(APP, "Back to Home dashboard", HOME_PROCESS),

            // submitField sends only the search input (execute=field), triggering a server-side
            // DataTable re-query — unlike the process widget, which filters client-side in JS.
            submitField(APP, "Quick-search tasks for 'Maternity'",
                    "task-task_1:quick-search-form", "task-task_1:quick-search-form:quick-search-input-0", "Maternity")
                .updates("task-task_1:task-component:dashboard-tasks", "task-task_1:task-component:empty-message-container")
                .assertOk().build(),

            // Portal's menu click returns a JSF <redirect> to the dashboard page; menuClick captures it
            // with .expectRedirect() and openRedirect follows it — no dashboard URL is hardcoded.
            menuClick("Navigate to Task List", AppMenu.TASK_LIST),
            openRedirect(APP, "Open Task List dashboard"),
            autoLoad(APP, "Load task-list widget",
                    "task-default_task_list_dashboard_task_1:task-component:rcLoadTaskFirstTime")
                .updates("task-default_task_list_dashboard_task_1:task-component:dashboard-tasks-container")
                .assertOk().build(),

            menuClick("Navigate to Case List", AppMenu.CASE_LIST),
            openRedirect(APP, "Open Case List dashboard"),
            autoLoad(APP, "Load case-list widget",
                    "case-default_case_list_dashboard_case_1:case-component:rcLoadCaseFirstTime")
                .updates("case-default_case_list_dashboard_case_1:case-component:dashboard-cases-container")
                .assertOk().build()

            // , logout(APP, "logout-setting:logout-menu-item").assertOk().build()
        )
        .run();
  }

  // ============================================================================
  // JOURNEY HELPERS — each helper is one step of the user journey.
  // Edit only if the shape of a step changes (e.g. different menu layout).
  // ============================================================================

  /**
   * Clicks a main-menu entry. Portal responds with a JSF {@code <redirect>} to the target dashboard
   * page; {@code .expectRedirect()} captures that URL into the session's redirect var so the caller
   * can follow it with {@code openRedirect(...)} — no dashboard URL needs to be hardcoded.
   */
  private static DslHttpSampler menuClick(String label, AppMenu menu) {
    return clickButton(APP, label, MAIN_MENU)
        .updates(MAIN_MENU)
        .param("taskId", "")
        .param("isWorkingOnATask", "false")
        .param("menuKind", menu.kind)
        .param("menuUrl", "")
        .param(MAIN_MENU + "_menuid", menu.menuId)
        .expectRedirect()
        .assertOk()
        .build();
  }
}
