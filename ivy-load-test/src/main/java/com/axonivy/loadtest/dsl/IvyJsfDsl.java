package com.axonivy.loadtest.dsl;

import com.axonivy.loadtest.dsl.config.IvyAppConfig;
import com.axonivy.loadtest.dsl.jsf.JsfAjax;
import com.axonivy.loadtest.dsl.jsf.JsfAssertions;
import com.axonivy.loadtest.dsl.jsf.JsfExtractors;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Static-import facade providing generic, fluent building blocks for load-testing Axon Ivy / JSF
 * (PrimeFaces) applications on top of jmeter-java-dsl.
 *
 * <p>All methods return jmeter-dsl types so they drop straight into an existing
 * {@code testPlan(threadGroup(...).children(...))} structure. The ViewState / form-action / redirect
 * correlation that normally has to be hand-wired with regex extractors is generated automatically.
 *
 * <p>Building blocks — pick by what the user or browser does:
 * <ul>
 *   <li>{@link #openProcess} — enter the app / open an Ivy process start link ({@code .ivp}).</li>
 *   <li>{@link #openRedirect} — follow a JSF {@code <redirect>} (pair with a POST's {@code .expectRedirect()}).</li>
 *   <li>{@link #openPage} — GET any already-correlated URL, e.g. a link href you extracted yourself.</li>
 *   <li>{@link #clickButton} — click a command button or link (no form fields).</li>
 *   <li>{@link #autoLoad} — a PrimeFaces remoteCommand that fires automatically after a page renders.</li>
 *   <li>{@link #submitForm} — submit a whole form ({@code execute=@all}); supply inputs with {@code .field(...)}.</li>
 *   <li>{@link #submitField} — a single input posting its own value (search box, column filter, on-change).</li>
 *   <li>{@link #login} — login form submit; supply form/command ids and chain {@code .field(...)} for credentials.</li>
 *   <li>{@link #logout} — logout button click; supply the button id.</li>
 *   <li>{@link #jsfAjax} — raw AJAX builder: the escape hatch when none of the presets above fit.</li>
 * </ul>
 *
 * <p>Builder chaining options ({@code .updates(...)}, {@code .expectRedirect()}, {@code .url(...)},
 * {@code .reExtractViewState()} for a POST&rarr;POST on one view, …) live on {@link JsfAjax}. Drop to
 * the raw correlation/assertion helpers {@link JsfExtractors} / {@link JsfAssertions} only when you
 * hand-build a sampler and need something the presets don't wire for you.
 *
 * <p>Intended usage:
 * <pre>{@code
 * import static com.axonivy.loadtest.dsl.IvyJsfDsl.*;
 *
 * IvyAppConfig app = IvyAppConfig.builder()
 *     .securitySystem("${__P(security.system.name)}")
 *     .application("${__P(application.name)}")
 *     .project("${__P(project.name)}")
 *     .build();
 *
 * threadGroup(...).children(
 *     openProcess(app, "Home", "DefaultApplicationHomePage.ivp"),
 *     login(app, "login:login-form", "login-form:login-command")
 *         .field("login:login-form:username", "${username}")
 *         .field("login:login-form:password", "${password}")
 *         .assertOk().build(),
 *     // A navigation = a redirect-emitting click, then a GET that follows it:
 *     clickButton(app, "Open Task List", "nav:menu")
 *         .param("nav:menu_menuid", "tasks")
 *         .expectRedirect().assertOk().build(),
 *     openRedirect(app, "Task List"),
 *     logout(app, "logout-button-id").assertOk().build());
 * }</pre>
 */
public final class IvyJsfDsl {

  private IvyJsfDsl() {
  }

  /**
   * GETs an Ivy process start page ({@code .ivp}) and auto-extracts the form-action URL and the
   * (full-HTML) ViewState for use by subsequent requests.
   */
  public static DslHttpSampler openProcess(IvyAppConfig session, String name, String processName) {
    return openPage(session, name, session.ivpUrl(processName));
  }

  /**
   * GETs an already-correlated URL (e.g. {@code "${redirectURL}"} after a JSF redirect) and
   * re-extracts the form-action URL and ViewState.
   */
  public static DslHttpSampler openPage(IvyAppConfig session, String name, String urlExpr) {
    return httpSampler(name, urlExpr).method("GET")
        .children(
            JsfExtractors.formAction(session.formActionVar()),
            JsfExtractors.viewState(session.viewStateVar()),
            JsfAssertions.ok());
  }

  /**
   * GETs the target of a JSF AJAX {@code <redirect>} captured by {@link JsfAjax#expectRedirect()},
   * re-correlating the form-action URL and ViewState for the page that follows. Pair it with a
   * redirect-emitting POST to model a navigation as two plain, composable steps:
   *
   * <pre>{@code
   * clickButton(app, "Start process", "...:start-button").processes("@all").expectRedirect().assertOk().build(),
   * openRedirect(app, "Open started process")
   * }</pre>
   */
  public static DslHttpSampler openRedirect(IvyAppConfig session, String name) {
    return openPage(session, name, "${" + session.redirectVar() + "}");
  }

  /**
   * Models a login form submit. Supply the form and command ids from your recording, then chain
   * {@link JsfAjax#field(String, String)} for the username and password inputs.
   *
   * <pre>{@code
   * login(app, "login:login-form", "login-form:login-command")
   *     .field("login:login-form:username", "${username}")
   *     .field("login:login-form:password", "${password}")
   *     .assertOk().build()
   * }</pre>
   */
  public static JsfAjax login(IvyAppConfig session, String formId, String commandId) {
    return submitForm(session, "Login", formId, commandId);
  }

  /**
   * Models a logout button click. Supply the button id from your recording.
   * Automatically sets {@code execute=@all} — required for Portal's logout to invalidate the session.
   *
   * <pre>{@code
   * logout(app, "logout-setting:logout-menu-item").assertOk().build()
   * }</pre>
   */
  public static JsfAjax logout(IvyAppConfig session, String buttonId) {
    return clickButton(session, "Logout", buttonId)
        .processes("@all");
  }

  /** Starts a fluent JSF AJAX POST builder bound to the given session. */
  public static JsfAjax jsfAjax(IvyAppConfig session, String name, String source) {
    return new JsfAjax(session, name, source);
  }

  /**
   * Models a JSF command-button or command-link click (no form fields).
   *
   * <p>Defaults: process the source component itself ({@code execute=buttonId}) and include the
   * PrimeFaces self-referential param ({@code selfParam}). No render target is set by default —
   * chain {@link JsfAjax#updates(String...)} if the response re-renders a section of the page, or
   * {@link JsfAjax#processes(String...)} to override which components are processed server-side.
   *
   * <pre>{@code
   * clickButton(app, "Navigate Tasks", "nav:menu")
   *     .updates("nav:menu")
   *     .param("nav:menu_menuid", "tasks")
   *     .assertOk().build()
   * }</pre>
   */
  public static JsfAjax clickButton(IvyAppConfig session, String name, String buttonId) {
    return jsfAjax(session, name, buttonId)
        .execute(buttonId)
        .selfParam();
  }

  /**
   * Models a PrimeFaces {@code <p:remoteCommand autoRun="true">} or any AJAX call that fires
   * automatically after a page renders (widget data loads, init-data commands, etc.) — as opposed
   * to a user gesture.
   *
   * <p>The HTTP shape is identical to {@link #clickButton}: process the source component itself
   * ({@code execute=componentId}) with the PrimeFaces self-referential param. The distinct name
   * makes the intent clear at the call site: this is a browser-triggered auto-load, not a click.
   *
   * <pre>{@code
   * autoLoad(app, "Task widget", "task-task_1:task-component:rcLoadTaskFirstTime")
   *     .updates("task-task_1:task-component:dashboard-tasks-container")
   *     .assertOk().build()
   * }</pre>
   */
  public static JsfAjax autoLoad(IvyAppConfig session, String name, String componentId) {
    return jsfAjax(session, name, componentId)
        .execute(componentId)
        .selfParam();
  }

  /**
   * Models a JSF form submit: processes all form fields ({@code execute=@all}), renders the form,
   * and automatically includes the PrimeFaces form-submit marker and self-referential param.
   *
   * <p>Use {@link JsfAjax#field(String, String)} to supply input values. Chain
   * {@link JsfAjax#assertOk()} and call {@link JsfAjax#build()} to finalise.
   *
   * <pre>{@code
   * submitForm(app, "Login", "login:login-form", "login-form:login-command")
   *     .field("login:login-form:username", "${username}")
   *     .field("login:login-form:password", "${password}")
   *     .assertOk().build()
   * }</pre>
   */
  public static JsfAjax submitForm(IvyAppConfig session, String name, String formId, String buttonId) {
    return jsfAjax(session, name, buttonId)
        .execute("@all")
        .render(formId)
        .selfParam()
        .formSubmit(formId);
  }

  /**
   * Models an AJAX submit triggered by a single input field — a search box, a column filter, an
   * on-change listener. Unlike {@link #submitForm}, the field processes only itself
   * ({@code execute=fieldId}) and posts its own value ({@code fieldId=value}) rather than the whole
   * form; the form-submit marker is still included. Chain {@link JsfAjax#updates(String...)} to name
   * the region the response re-renders.
   *
   * <pre>{@code
   * submitField(app, "Quick-search", "tasks:search-form", "tasks:search-form:input", "Maternity")
   *     .updates("tasks:results", "tasks:empty-message")
   *     .assertOk().build()
   * }</pre>
   */
  public static JsfAjax submitField(IvyAppConfig session, String name, String formId, String fieldId,
      String value) {
    return jsfAjax(session, name, fieldId)
        .execute(fieldId)
        .formSubmit(formId)
        .field(fieldId, value);
  }

}
