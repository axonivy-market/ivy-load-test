package com.axonivy.loadtest.dsl.jsf;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;

import com.axonivy.loadtest.dsl.config.IvyAppConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import us.abstracta.jmeter.javadsl.core.samplers.BaseSampler;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

/**
 * Fluent builder for a JSF/PrimeFaces partial (AJAX) POST.
 *
 * <p>It auto-emits the {@code javax.faces.partial.*} parameters and injects the current
 * {@code javax.faces.ViewState} so callers express only intent (the source component, the form
 * fields, what to render). Build it into a {@link DslHttpSampler} with {@link #build()} so it
 * composes inside {@code threadGroup(...)} / {@code transaction(...)} exactly like a raw sampler.
 *
 * <p>Component ids are entirely caller-supplied — this class is app-agnostic.
 */
public final class JsfAjax {

  private final IvyAppConfig session;
  private final String name;
  private final String source;

  private String urlExpr;
  private String execute = "@all";
  private String render;
  private final Map<String, String> params = new LinkedHashMap<>();
  private String formId;
  private boolean selfParam;
  private boolean assertOk;
  private boolean expectRedirect;
  private boolean reExtractViewState;

  public JsfAjax(IvyAppConfig session, String name, String source) {
    this.session = session;
    this.name = name;
    this.source = source;
    this.urlExpr = "${" + session.formActionVar() + "}";
  }

  /** Override the request URL (defaults to the correlated form-action variable, e.g. {@code "${url}"}). */
  public JsfAjax url(String urlExpr) {
    this.urlExpr = urlExpr;
    return this;
  }

  /** Sets {@code javax.faces.partial.execute} (space-joined). Defaults to {@code @all}. */
  public JsfAjax execute(String... ids) {
    this.execute = String.join(" ", ids);
    return this;
  }

  /** Sets {@code javax.faces.partial.render} (space-joined). Omitted entirely when not called. */
  public JsfAjax render(String... ids) {
    this.render = String.join(" ", ids);
    return this;
  }

  /** Emits the common {@code <source>=<source>} self-referential parameter. */
  public JsfAjax selfParam() {
    this.selfParam = true;
    return this;
  }

  /** Adds an arbitrary form parameter. */
  public JsfAjax param(String name, String value) {
    this.params.put(name, value);
    return this;
  }

  /**
   * Alias for {@link #param} — adds a form-field value. Prefer this name inside
   * {@code submitForm(...)} chains to make the intent explicit.
   */
  public JsfAjax field(String name, String value) {
    return param(name, value);
  }

  /**
   * Alias for {@link #execute} — names which components the server should process.
   * More readable than the JSF wire-protocol name.
   */
  public JsfAjax processes(String... ids) {
    return execute(ids);
  }

  /**
   * Alias for {@link #render} — names which components should be updated in the response.
   * More readable than the JSF wire-protocol name.
   */
  public JsfAjax updates(String... ids) {
    return render(ids);
  }

  /** Emits {@code <formId>_SUBMIT=1} for the given JSF form. */
  public JsfAjax formSubmit(String formId) {
    this.formId = formId;
    return this;
  }

  /** Attaches a response-code 200 assertion. */
  public JsfAjax assertOk() {
    this.assertOk = true;
    return this;
  }

  /** Attaches an extractor for the JSF AJAX {@code <redirect url="..."/>} into the session's redirect var. */
  public JsfAjax expectRedirect() {
    this.expectRedirect = true;
    return this;
  }

  /**
   * Re-extracts a fresh ViewState from this POST's partial-response into the session's ViewState var.
   * Off by default (the typical navigate-then-GET flow re-correlates on the following page load).
   * Enable for POST&rarr;POST sequences on the same JSF view.
   */
  public JsfAjax reExtractViewState() {
    this.reExtractViewState = true;
    return this;
  }

  /** Materializes the configured AJAX POST as a composable {@link DslHttpSampler}. */
  public DslHttpSampler build() {
    DslHttpSampler sampler = httpSampler(name, urlExpr).method("POST")
        .param("javax.faces.partial.ajax", "true")
        .param("javax.faces.source", source)
        .param("javax.faces.partial.execute", execute);
    if (render != null) {
      sampler = sampler.param("javax.faces.partial.render", render);
    }
    if (selfParam) {
      sampler = sampler.param(source, source);
    }
    for (Map.Entry<String, String> e : params.entrySet()) {
      sampler = sampler.param(e.getKey(), e.getValue());
    }
    if (formId != null) {
      sampler = sampler.param(formId + "_SUBMIT", "1");
    }
    sampler = sampler.param("javax.faces.ViewState", "${" + session.viewStateVar() + "}");

    List<BaseSampler.SamplerChild> children = new ArrayList<>();
    if (expectRedirect) {
      children.add(JsfExtractors.redirect(session.redirectVar()));
    }
    if (reExtractViewState) {
      children.add(JsfExtractors.viewStatePartial(session.viewStateVar()));
    }
    if (assertOk) {
      children.add(JsfAssertions.ok());
    }
    if (!children.isEmpty()) {
      sampler = sampler.children(children.toArray(new BaseSampler.SamplerChild[0]));
    }
    return sampler;
  }
}
