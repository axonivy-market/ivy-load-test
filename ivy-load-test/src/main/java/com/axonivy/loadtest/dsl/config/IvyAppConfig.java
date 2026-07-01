package com.axonivy.loadtest.dsl.config;

/**
 * Immutable configuration for an Axon Ivy load-test target.
 *
 * <p>Holds the connection coordinates plus the pieces needed to build an Ivy process ({@code .ivp})
 * URL, and the names of the JMeter variables used to correlate JSF state between requests. Values
 * may be literal or JMeter property/variable placeholders (e.g. {@code "${__P(server.host)}"}); they
 * are stored verbatim so the existing {@code test.properties} mechanism keeps working unchanged.
 *
 * <p>This object is deliberately app-agnostic: it knows nothing about Portal component ids. Build one
 * with {@link #builder()} and pass it to {@code com.axonivy.loadtest.dsl.IvyJsfDsl} helpers.
 */
public final class IvyAppConfig {

  private final String host;
  private final String port;
  private final String securitySystem;
  private final String application;
  private final String project;
  private final String processHash;
  private final String viewStateVar;
  private final String formActionVar;
  private final String redirectVar;

  private IvyAppConfig(Builder b) {
    this.host = b.host;
    this.port = b.port;
    this.securitySystem = b.securitySystem;
    this.application = b.application;
    this.project = b.project;
    this.processHash = b.processHash;
    this.viewStateVar = b.viewStateVar;
    this.formActionVar = b.formActionVar;
    this.redirectVar = b.redirectVar;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Returns a new builder pre-populated with this config's values, ready to override specific fields. */
  public Builder toBuilder() {
    Builder b = new Builder();
    b.host = this.host;
    b.port = this.port;
    b.securitySystem = this.securitySystem;
    b.application = this.application;
    b.project = this.project;
    b.processHash = this.processHash;
    b.viewStateVar = this.viewStateVar;
    b.formActionVar = this.formActionVar;
    b.redirectVar = this.redirectVar;
    return b;
  }

  /**
   * Builds the relative URL of an Ivy process start link:
   * {@code /{securitySystem}/{application}/pro/{project}/{processHash}/{processName}}.
   * The host/port are supplied separately via {@code httpDefaults()} in the test plan.
   *
   * @param processName e.g. {@code "DefaultApplicationHomePage.ivp"}
   */
  public String ivpUrl(String processName) {
    return "/" + securitySystem + "/" + application + "/pro/" + project + "/" + processHash + "/" + processName;
  }

  public String host() {
    return host;
  }

  public String port() {
    return port;
  }

  public String securitySystem() {
    return securitySystem;
  }

  public String application() {
    return application;
  }

  public String project() {
    return project;
  }

  public String processHash() {
    return processHash;
  }

  /** Name of the JMeter variable that holds the current JSF ViewState (default {@code "viewState"}). */
  public String viewStateVar() {
    return viewStateVar;
  }

  /** Name of the JMeter variable that holds the current form action URL (default {@code "url"}). */
  public String formActionVar() {
    return formActionVar;
  }

  /** Name of the JMeter variable that holds the last JSF AJAX redirect URL (default {@code "redirectURL"}). */
  public String redirectVar() {
    return redirectVar;
  }

  public static final class Builder {
    private String host = "localhost";
    private String port;
    private String securitySystem = "";
    private String application;
    private String project;
    private String processHash = "1549F58C18A6C562";
    private String viewStateVar = "viewState";
    private String formActionVar = "url";
    private String redirectVar = "redirectURL";

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = String.valueOf(port);
      return this;
    }

    /** Accepts a literal port or a JMeter expression, e.g. {@code "${__P(server.port)}"}. */
    public Builder port(String port) {
      this.port = port;
      return this;
    }

    /** Security system path segment; keep empty on a local Designer environment. */
    public Builder securitySystem(String securitySystem) {
      this.securitySystem = securitySystem;
      return this;
    }

    public Builder application(String application) {
      this.application = application;
      return this;
    }

    public Builder project(String project) {
      this.project = project;
      return this;
    }

    /** The opaque Ivy process-element hash in the {@code .ivp} URL. Override per target app. */
    public Builder processHash(String processHash) {
      this.processHash = processHash;
      return this;
    }

    public Builder viewStateVar(String viewStateVar) {
      this.viewStateVar = viewStateVar;
      return this;
    }

    public Builder formActionVar(String formActionVar) {
      this.formActionVar = formActionVar;
      return this;
    }

    public Builder redirectVar(String redirectVar) {
      this.redirectVar = redirectVar;
      return this;
    }

    public IvyAppConfig build() {
      return new IvyAppConfig(this);
    }
  }
}
