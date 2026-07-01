# Ivy Load Test

Load-test your Axon Ivy application in minutes. Writing a JMeter test for a JSF/PrimeFaces app by hand means hunting down component ids in DevTools, hand-wiring ViewState for every AJAX call, and maintaining brittle XML. This library does all of that for you — copy a template, tell it where your app lives and what to click, and it generates the correct JMeter test plan automatically.

## How it works

You describe the user journey in plain Java — the library translates it to a complete JMeter test plan:

```java
IvyLoadTestRunner.builder(APP, "my-scenario")
    .steps(
        openProcess(APP, "Open Home", HOME_PROCESS),
        login(APP, "login:login-form", "login-form:login-command")
            .field("login:login-form:username", "${username}")
            .field("login:login-form:password", "${password}")
            .assertOk().build(),
        menuClick("Task list", AppMenu.TASK_LIST),
        openRedirect(APP, "Task list"),
        logout(APP, "logout-setting:logout-menu-item").assertOk().build())
    .run();
```

User count, ramp-up, and the credentials CSV default to your `test.properties`, so a scenario usually only sets `.steps(...)`; chain `.users(...)`, `.rampUp(...)`, or `.csvData(...)` to override any of them. The `ivy-load-test` library takes care of all JSF/PrimeFaces wire details (ViewState tracking, AJAX request format, redirect follow-through) so you never have to touch them.

## Repository layout

| Module | What it is |
|---|---|
| `ivy-load-test` | The library — `IvyJsfDsl` step builders and `IvyLoadTestRunner` engine wiring |
| `ivy-load-test-demo` | **Start here.** Contains `LoadTestTemplate` (minimal skeleton) and `PortalWalkthroughLoadTest` (full worked example) |
| `ivy-load-test-product` | Product packaging — this README and release configuration |

Work in `ivy-load-test-demo`. The DSL module is a dependency you never touch unless you are extending the library itself.

## Quick Start

1. Clone the repository.
2. From the repository root, install all modules into your local Maven cache:
```bash
mvn install -DskipTests
```

## Run the Portal example

`PortalWalkthroughLoadTest` is a complete Portal walkthrough — open the home dashboard, load its widgets, navigate the Processes page, start a process, quick-search the task list, and visit the task & case dashboards.

**Prerequisites:**
- Axon Ivy Designer running locally with Portal installed
- A valid Portal user — add your credentials to `resources/one_user.csv` (replace the placeholder row). The file has no header row — one `username,password` pair per line:

```
yourUsername,yourPassword
```

**Run:**
```bash
mvn install -Plocal-portal -Dtest=PortalWalkthroughLoadTest
```

> These tests need a running Portal and are excluded from the default CI build. The `-Plocal-portal` profile re-includes them and rebuilds the DSL first — run this from the repository root.

## Record a scenario instead of hand-collecting ids

Rather than reading component ids from your browser's DevTools, record a real browser session and let the tool print the values *and* the journey for you (needs [jbang](https://www.jbang.dev)):

```bash
./record.sh http://your-host:8081/      # Linux/macOS   (Windows: .\record.ps1 http://your-host:8081/)
```

A browser opens — walk through your scenario, then **close the browser window** to finish. It prints the `processHash`, `HOME_PROCESS`, the menu rows, and every JSF component id you clicked, ready to paste into the `CUSTOMIZE FOR YOUR APP` block. It also prints the **ordered journey** — each click annotated with the `execute` / `render` / `form` it actually sent, so you can write the `jsfAjax(...)` call yourself. This is a *reference, not a finished test*: walk the journey against your own blueprint, implement the steps you intended, and skip the `[background]` / `[remote-command]` noise. Re-read an existing capture anytime with `jbang RecordIds.java recording`.

**What it can and can't see** — the recorder is an HTTP proxy, so it only captures what crosses the network:

- ✅ page opens (`.ivp` / `.xhtml`), AJAX clicks, menu navigation
- ❌ **client-side filters / search** — typing in a list that filters in the browser sends no request, so nothing is recorded
- ❌ **the top-right profile menu** (Dashboard configuration, My profile, …) and **logout** — the proxy captures the main navigation but does not capture these profile-menu / end-of-session navigations; add them by hand
- ⚠️ auto-generated ids (`j_id_…`) can change when the app is redeployed — re-record if a step starts failing

Use HTTP (not HTTPS) and accept the JMeter proxy certificate in the browser, otherwise requests bypass the proxy and go unrecorded.

## Adapt to your own app

`LoadTestTemplate` is the copy-paste starting point — a minimal skeleton (app config + one `openProcess` step, with the full menu of step builders shown inline as comments). `PortalWalkthroughLoadTest` is the complete worked example. Wherever you start, the two things to change are:

### 1. `resources/test.properties`

```properties
server.host=localhost          # your server
server.port=8081               # your server port
load.users=1                   # virtual users to ramp to
load.rampup=1                  # ramp-up period in seconds
application.name=designer      # your Ivy application name
project.name=portal            # your Ivy project name
security.system.name=          # leave empty for a local Designer environment
one_user.csv=resources/one_user.csv
```

### 2. The `CUSTOMIZE FOR YOUR APP` block in your test class

```java
// a) Your app coordinates
private static final IvyAppConfig APP = IvyAppConfig.builder()
    .host("${__P(server.host)}")
    .port("${__P(server.port)}")
    .application("${__P(application.name)}")
    .project("${__P(project.name)}")
    .processHash("YOUR_PROCESS_HASH")    // the hash in the .ivp URL — visible in Designer
    .build();

// b) Your entry process (.ivp file opened at start and after login)
private static final String HOME_PROCESS = "YourHomePage.ivp";

// c) Menu entries — one row per section your scenario visits
private enum AppMenu {
    TASK_LIST("main_dashboard", "tasks-menu-id"),
    ...
}
```

`LoadTestTemplate` ships with just (a) and (b) — the minimal start to copy. The `AppMenu` enum (c) is what a fuller scenario adds; `PortalWalkthroughLoadTest` shows it in use.

### 3. Your scenario (optional)

The default scenario covers login → navigate → logout. Add, remove, or reorder steps in the `@Test` method as needed — each step is a readable one-liner:

```java
@Test
public void myScenario() throws Exception {
    IvyLoadTestRunner.builder(APP, "my-scenario")
        .steps(
            openProcess(APP, "Open Home", HOME_PROCESS),
            login(APP, "login:login-form", "login-form:login-command")
                .field("login:login-form:username", "${username}")
                .field("login:login-form:password", "${password}")
                .assertOk().build(),
            menuClick("Task list", AppMenu.TASK_LIST),
            openRedirect(APP, "Task list"),
            logout(APP, "logout-setting:logout-menu-item").assertOk().build())
        .run();
}
```

`login()` and `logout()` are DSL helpers — supply the component ids from your recording. `menuClick()` is a private helper at the bottom of the class; only edit it if your app's menu layout differs.

## Credentials

The CSV file has no header row — one virtual user per line. The `username,password` column names are supplied by the DSL, so the first line is already read as credentials:

```csv
yourUsername,yourPassword
```

**Never commit real credentials to version control.** On CI, inject them as a secret file:

```groovy
// Jenkinsfile
withCredentials([file(credentialsId: 'load-test-users', variable: 'USERS_CSV')]) {
    sh 'cp "$USERS_CSV" ivy-load-test-demo/resources/one_user.csv'
}
```

**Security checklist:**
- Add `resources/*.csv` to `.gitignore`
- Use a dedicated test account with minimum required permissions
- Rotate the test account password regularly

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Login fails / 401 errors | Wrong credentials in CSV, or the CSV accidentally has a header row (no header is expected, so the first line is read as credentials) |
| All samples fail immediately | App is not running, or `server.host` / port is wrong |
| Navigation steps fail | `processHash` in `IvyAppConfig` does not match your app — copy it from the `.ivp` URL in Designer |
| Connection refused on CI | The test machine cannot reach the app server — check firewall / network config |

For detailed request/response inspection during local development, uncomment `resultsTreeVisualizer()` inside `IvyLoadTestRunner`.
