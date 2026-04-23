package com.axonivy;

import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.engines.EmbeddedJmeterEngine;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static us.abstracta.jmeter.javadsl.JmeterDsl.*;
import static us.abstracta.jmeter.javadsl.core.assertions.DslResponseAssertion.TargetField;

public class PerformancePortalTest {


  private String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

  @Test
  public void testPortalWalkthrough() throws IOException, InterruptedException, TimeoutException {
    // ====================Test one admin user====================
    TestPlanStats stats1User = runPortalTest(1, 1,"1_admin_user", "${__P(one_user.csv)}");
    validateTestResults(stats1User, "1 admin user test");
  }

  private TestPlanStats runPortalTest(int numberOfUsers, int rampUpPeriod, String testName, String csvFilePath) throws IOException, InterruptedException, TimeoutException {
    String jtlDirName = String.format("target/jtls/%s", timestamp);

    return testPlan(
      threadGroup(testName)
        .rampTo(numberOfUsers,                 // Number of users
          Duration.ofSeconds(rampUpPeriod))    // Ramp up period
        .holdIterating(1)                      // portal.thread.loop
        .children(
          httpDefaults()
              .host("${__P(server.host)}")
              .port(8081),
          httpCookies(),

          httpHeaders().header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36"),

          csvDataSet(csvFilePath)
            .variableNames("username,password")
            .delimiter(",")
            .ignoreFirstLine(false),

          httpSampler("PortalStart",
            "/${__P(security.system.name)}/${__P(application.name)}/pro/${__P(project.name)}/1549F58C18A6C562/DefaultApplicationHomePage.ivp")
            .method("GET")
            .children(
              regexExtractor("url", "action=\"([^\"]+)\""),
              regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") ")
            ),

          httpSampler("Login"
            ,"${url}")
            .method("POST")
            .param("javax.faces.partial.ajax", "true")
            .param("javax.faces.source", "login-form:login-command")
            .param("javax.faces.partial.execute", "@all")
            .param("javax.faces.partial.render", "login:login-form")
            .param("login:login-form:login-command", "login:login-form:login-command")
            .param("login:login-form:username", "${username}")
            .param("login:login-form:password", "${password}")
            .param("login:login-form_SUBMIT", "1")
            .param("javax.faces.ViewState", "${viewState}")
            .children(
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("PortalHome"
            ,"/${__P(security.system.name)}/${__P(application.name)}/pro/${__P(project.name)}/1549F58C18A6C562/DefaultApplicationHomePage.ivp")
            .method("GET")
            .children(
              regexExtractor("url", "action=\"([^\"]+)\""),
              regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") "),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("NavigateToProcesses"
            ,"${url}")
            .method("POST")
            .param("javax.faces.partial.ajax", "true")
            .param("javax.faces.source", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.execute", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.render", "user-menu-required-login:main-navigator:main-menu")
            .param("user-menu-required-login:main-navigator:main-menu", "user-menu-required-login:main-navigator:main-menu")
            .param("taskId", "")
            .param("isWorkingOnATask", "false")
            .param("menuKind", "process")
            .param("menuUrl", "")
            .param("user-menu-required-login:main-navigator:main-menu_menuid", "process_1")
            .param("javax.faces.ViewState", "${viewState}")
            .children(
              regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">"),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("PortalProcesses"
            ,"${redirectURL}")
            .method("GET")
            .children(
              regexExtractor("url", "action=\"([^\"]+)\""),
              regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") "),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("NavigateToTaskList"
            ,"${url}")
            .method("POST")
            .param("javax.faces.partial.ajax", "true")
            .param("javax.faces.source", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.execute", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.render", "user-menu-required-login:main-navigator:main-menu")
            .param("user-menu-required-login:main-navigator:main-menu", "user-menu-required-login:main-navigator:main-menu")
            .param("taskId", "")
            .param("isWorkingOnATask", "false")
            .param("menuKind", "main_dashboard")
            .param("menuUrl", "")
            .param("user-menu-required-login:main-navigator:main-menu_menuid", "_js__default-task-list-dashboard-main-dashboard")
            .param("javax.faces.ViewState", "${viewState}")
            .children(
              regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">"),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("PortalTaskList"
            ,"${redirectURL}")
            .method("GET")
            .children(
              regexExtractor("url", "action=\"([^\"]+)\""),
              regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\")"),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),


          httpSampler("NavigateToCaseList"
            ,"${url}")
            .method("POST")
            .param("javax.faces.partial.ajax", "true")
            .param("javax.faces.source", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.execute", "user-menu-required-login:main-navigator:main-menu")
            .param("javax.faces.partial.render", "user-menu-required-login:main-navigator:main-menu")
            .param("user-menu-required-login:main-navigator:main-menu", "user-menu-required-login:main-navigator:main-menu")
            .param("taskId", "")
            .param("isWorkingOnATask", "false")
            .param("menuKind", "main_dashboard")
            .param("menuUrl", "")
            .param("user-menu-required-login:main-navigator:main-menu_menuid", "_js__default-case-list-dashboard-main-dashboard")
            .param("javax.faces.ViewState", "${viewState}")
            .children(
              regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">"),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("PortalCaseList"
            ,"${redirectURL}")
            .method("GET")
            .children(
              regexExtractor("url", "action=\"([^\"]+)\""),
              regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\")"),
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            ),

          httpSampler("Logout"
            ,"${url}")
            .method("POST")
            .param("javax.faces.partial.ajax", "true")
            .param("javax.faces.source", "logout-setting:logout-menu-item")
            .param("javax.faces.partial.execute", "@all")
            .param("logout-setting:logout-menu-item", "logout-setting:logout-menu-item")
            .param("javax.faces.ViewState", "${viewState}")
            .children(
              responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
            )
        ),
      // Remove comment the line below on local environment to debug
      // resultsTreeVisualizer(),

      jtlWriter(jtlDirName, testName + ".jtl"),
      htmlReporter("target/html-report/" + testName)
    ).runIn(new EmbeddedJmeterEngine().propertiesFile("resources/test.properties"));
  }
  
  private void validateTestResults(TestPlanStats stats, String testDescription) {
    // Validate test results - fail the test if there are any errors
    if (stats.overall().errorsCount() > 0) {
      double errorPercentage = (double) stats.overall().errorsCount() / stats.overall().samplesCount() * 100;
      fail(String.format("%s failed with %d errors out of %d samples (%.2f%% error rate)", 
           testDescription,
           stats.overall().errorsCount(), 
           stats.overall().samplesCount(), 
           errorPercentage));
    } else {
      System.out.printf("%s completed successfully: %d samples, 0 errors%n", testDescription, stats.overall().samplesCount());
    }
  }

}
