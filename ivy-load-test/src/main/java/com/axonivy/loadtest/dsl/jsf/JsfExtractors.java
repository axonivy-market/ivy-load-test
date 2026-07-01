package com.axonivy.loadtest.dsl.jsf;

import static us.abstracta.jmeter.javadsl.JmeterDsl.regexExtractor;

import us.abstracta.jmeter.javadsl.core.postprocessors.DslRegexExtractor;

/**
 * The single place where the brittle JSF/PrimeFaces correlation regexes live.
 *
 * <p>Each method returns a post-processor that can be attached as a child of an HTTP sampler. Keeping
 * these patterns in one spot means a JSF markup change is a one-line fix instead of a project-wide
 * find-and-replace.
 */
public final class JsfExtractors {

  private JsfExtractors() {
  }

  /**
   * Extracts the JSF ViewState token from a full HTML response (a hidden input rendered on a normal
   * page GET). Captures the bare token (no surrounding quotes) and is independent of the JSF naming
   * container prefix (e.g. {@code j_id__v_0}), which varies between views and Ivy versions.
   */
  public static DslRegexExtractor viewState(String varName) {
    return regexExtractor(varName, "id=\"[^\"]*javax\\.faces\\.ViewState[^\"]*\" value=\"([^\"]+)\"")
        .matchNumber(1);
  }

  /**
   * Extracts the JSF ViewState token from a PrimeFaces partial-response (AJAX) XML body, where it is
   * encoded as {@code <update id="...javax.faces.ViewState..."><![CDATA[token]]></update>}.
   * Use this only for POST&rarr;POST flows on the same view; pure-redirect AJAX responses carry no
   * ViewState update, in which case (with no default value) the existing variable is left intact.
   */
  public static DslRegexExtractor viewStatePartial(String varName) {
    return regexExtractor(varName,
        "<update id=\"[^\"]*javax\\.faces\\.ViewState[^\"]*\"><!\\[CDATA\\[([^\\]]+)\\]\\]></update>")
        .matchNumber(1);
  }

  /** Extracts the {@code action="..."} attribute of the (first) form on a full HTML response. */
  public static DslRegexExtractor formAction(String varName) {
    return regexExtractor(varName, "action=\"([^\"]+)\"").matchNumber(1);
  }

  /** Extracts the target URL from a JSF AJAX {@code <redirect url="..."/>} partial-response. */
  public static DslRegexExtractor redirect(String varName) {
    return regexExtractor(varName, "<redirect url=\"([^\"]+)\"\\s*/?>").matchNumber(1);
  }
}
