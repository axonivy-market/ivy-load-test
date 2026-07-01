# Ivy Load Test

Testen Sie Ihre Axon Ivy-Anwendung auf Last — in wenigen Minuten. JMeter-Tests für eine JSF/PrimeFaces-Anwendung von Hand zu schreiben bedeutet, Komponenten-IDs in den DevTools zu suchen, für jeden AJAX-Aufruf manuell ViewState zu verdrahten und sprödes XML zu pflegen. Diese Bibliothek erledigt all das für Sie — kopieren Sie eine Vorlage, geben Sie an, wo Ihre App läuft und was angeklickt werden soll, und sie generiert automatisch den richtigen JMeter-Testplan.

## So funktioniert es

Sie beschreiben die Benutzerreise in normalem Java — die Bibliothek übersetzt das in einen vollständigen JMeter-Testplan:

```java
IvyLoadTestRunner.builder(APP, "mein-szenario")
    .steps(
        openProcess(APP, "Open Home", HOME_PROCESS),
        menuClick("Task list", AppMenu.TASK_LIST),
        openRedirect(APP, "Task list"),
        logout(APP, "logout-setting:logout-menu-item").assertOk().build())
    .run();
```

Benutzeranzahl, Ramp-up und die Zugangsdaten-CSV stammen standardmäßig aus Ihrer `test.properties`, sodass ein Szenario in der Regel nur `.steps(...)` setzt; mit `.users(...)`, `.rampUp(...)` oder `.csvData(...)` überschreiben Sie sie bei Bedarf. Die `ivy-load-test`-Bibliothek übernimmt alle JSF/PrimeFaces-Details (ViewState-Tracking, AJAX-Anforderungsformat, Weiterleitungen) — Sie müssen diese nie anfassen.

## Repository-Übersicht

| Modul | Beschreibung |
|---|---|
| `ivy-load-test` | Die Bibliothek — `IvyJsfDsl`-Schritt-Builder und `IvyLoadTestRunner`-Engine-Verdrahtung |
| `ivy-load-test-demo` | **Hier starten.** Enthält `LoadTestTemplate` (minimales Grundgerüst) und `PortalWalkthroughLoadTest` (vollständiges Beispiel) |
| `ivy-load-test-product` | Produkt-Packaging — dieses README und Release-Konfiguration |

Arbeiten Sie in `ivy-load-test-demo`. Das DSL-Modul ist eine Abhängigkeit, die Sie nicht anfassen müssen, sofern Sie die Bibliothek nicht erweitern.

## Schnellstart

1. Repository klonen.
2. Vom Repository-Stammverzeichnis alle Module in den lokalen Maven-Cache installieren:
```bash
mvn install -DskipTests
```

## Portal-Beispiel ausführen

`PortalWalkthroughLoadTest` ist ein vollständiger Portal-Durchlauf — Startseite öffnen, Dashboard-Widgets laden, Prozessseite navigieren, einen Prozess starten, Aufgabenliste durchsuchen und die Aufgaben- & Fall-Dashboards besuchen.

**Voraussetzungen:**
- Axon Ivy Designer läuft lokal mit installiertem Portal
- Ein gültiger Portal-Benutzer — Zugangsdaten in `resources/one_user.csv` eintragen (eine Zeile, kein Header):

```
benutzername,passwort
```

**Ausführen:**
```bash
mvn install -Plocal-portal -Dtest=PortalWalkthroughLoadTest
```

> Diese Tests benötigen ein laufendes Portal und sind vom Standard-CI-Build ausgeschlossen. Das Profil `-Plocal-portal` schließt sie wieder ein und baut das DSL zuerst neu — aus dem Repository-Stammverzeichnis ausführen.

## Szenario aufzeichnen statt IDs von Hand zu sammeln

Anstatt Komponenten-IDs aus den Browser-DevTools abzulesen, zeichnen Sie eine echte Browser-Sitzung auf und lassen Sie sich die Werte *und* die Journey ausgeben (benötigt [jbang](https://www.jbang.dev)):

```bash
./record.sh http://ihr-host:8081/      # Linux/macOS   (Windows: .\record.ps1 http://ihr-host:8081/)
```

Ein Browser öffnet sich — gehen Sie Ihr Szenario durch und **schließen Sie das Browserfenster**, um zu beenden. Es gibt den `processHash`, `HOME_PROCESS`, die Menüzeilen und jede angeklickte JSF-Komponenten-ID aus, bereit zum Einfügen in den Block `CUSTOMIZE FOR YOUR APP`. Außerdem wird die **geordnete Journey** ausgegeben — jeder Klick mit dem tatsächlich gesendeten `execute` / `render` / `form` annotiert, sodass Sie den `jsfAjax(...)`-Aufruf selbst schreiben können. Das ist eine *Referenz, kein fertiger Test*: gehen Sie die Journey gegen Ihr eigenes Blueprint durch, setzen Sie die beabsichtigten Schritte um und überspringen Sie das `[background]` / `[remote-command]`-Rauschen. Eine vorhandene Aufzeichnung jederzeit erneut auslesen mit `jbang RecordIds.java recording`.

**Was erfasst wird und was nicht** — der Recorder ist ein HTTP-Proxy und erfasst nur, was über das Netzwerk geht:

- ✅ Seitenaufrufe (`.ivp` / `.xhtml`), AJAX-Klicks, Menünavigation
- ❌ **clientseitige Filter / Suche** — Tippen in einer Liste, die im Browser filtert, sendet keine Anfrage und wird daher nicht aufgezeichnet
- ❌ **das Profilmenü oben rechts** (Dashboard-Konfiguration, Mein Profil, …) und **Logout** — der Proxy erfasst die Hauptnavigation, erfasst diese Profilmenü-/Sitzungsende-Navigationen aber nicht; diese von Hand ergänzen
- ⚠️ automatisch generierte IDs (`j_id_…`) können sich bei einem Redeploy ändern — bei fehlschlagendem Schritt erneut aufzeichnen

Verwenden Sie HTTP (nicht HTTPS) und akzeptieren Sie das JMeter-Proxy-Zertifikat im Browser, sonst umgehen Anfragen den Proxy und werden nicht aufgezeichnet.

## An Ihre eigene App anpassen

`LoadTestTemplate` ist der Copy-Paste-Ausgangspunkt — ein minimales Grundgerüst (App-Konfiguration + ein `openProcess`-Schritt, mit allen Schritt-Bausteinen als Kommentar). `PortalWalkthroughLoadTest` ist das vollständige Beispiel. Egal womit Sie beginnen, diese beiden Stellen ändern Sie:

### 1. `resources/test.properties`

```properties
server.host=localhost          # Ihr Server
server.port=8081               # Ihr Server-Port
load.users=1                   # Anzahl virtueller Benutzer
load.rampup=1                  # Ramp-up-Periode in Sekunden
application.name=designer      # Ihr Ivy-Anwendungsname
project.name=portal            # Ihr Ivy-Projektname
security.system.name=          # Für lokale Designer-Umgebung leer lassen
one_user.csv=resources/one_user.csv
```

### 2. Der Block `CUSTOMIZE FOR YOUR APP` in Ihrer Testklasse

```java
// a) Ihre App-Koordinaten
private static final IvyAppConfig APP = IvyAppConfig.builder()
    .host("${__P(server.host)}")
    .port("${__P(server.port)}")
    .application("${__P(application.name)}")
    .project("${__P(project.name)}")
    .processHash("IHR_PROZESS_HASH")     // der Hash in der .ivp-URL — im Designer sichtbar
    .build();

// b) Ihr Einstiegsprozess (.ivp-Datei, die beim Start und nach dem Login geöffnet wird)
private static final String HOME_PROCESS = "IhreStartseite.ivp";

// c) Menüeinträge — eine Zeile pro Bereich, den Ihr Szenario besucht
private enum AppMenu {
    TASK_LIST("main_dashboard", "tasks-menu-id"),
    ...
}
```

`LoadTestTemplate` enthält nur (a) und (b) — der minimale Startpunkt zum Kopieren. Das `AppMenu`-Enum (c) fügen Sie für ein umfangreicheres Szenario hinzu; `PortalWalkthroughLoadTest` zeigt es im Einsatz.

### 3. Ihr Szenario (optional)

Das Standardszenario umfasst Login → Navigation → Logout. Fügen Sie Schritte im `@Test`-Body hinzu, entfernen oder ordnen Sie sie um — jeder Schritt ist ein lesbarer Einzeiler:

```java
@Test
public void meinSzenario() throws Exception {
    IvyLoadTestRunner.builder(APP, "mein-szenario")
        .steps(
            openProcess(APP, "Open Home", HOME_PROCESS),
            menuClick("Task list", AppMenu.TASK_LIST),
            openRedirect(APP, "Task list"),
            logout(APP, "logout-setting:logout-menu-item").assertOk().build())
        .run();
}
```

`login()` und `logout()` sind DSL-Helfer — geben Sie die Komponenten-IDs aus Ihrer Aufzeichnung an. `menuClick()` ist ein privater Helfer am Ende der Klasse; bearbeiten Sie ihn nur, wenn das Menü-Layout Ihrer App abweicht.

## Zugangsdaten

Die CSV-Datei enthält einen virtuellen Benutzer pro Zeile, ohne Header:

```csv
benutzername,IhrPasswort
```

**Checken Sie niemals echte Zugangsdaten in die Versionskontrolle ein.** Auf CI als Secret-Datei injizieren:

```groovy
// Jenkinsfile
withCredentials([file(credentialsId: 'load-test-benutzer', variable: 'BENUTZER_CSV')]) {
    sh 'cp "$BENUTZER_CSV" ivy-load-test-demo/resources/one_user.csv'
}
```

**Sicherheits-Checkliste:**
- `resources/*.csv` zur `.gitignore` hinzufügen
- Dediziertes Testkonto mit minimalen Berechtigungen verwenden
- Passwort des Testkontos regelmäßig rotieren

## Fehlerbehebung

| Symptom | Wahrscheinliche Ursache |
|---|---|
| Login schlägt fehl / 401-Fehler | Falsche Zugangsdaten in CSV, oder CSV hat versehentlich eine Header-Zeile |
| Alle Samples schlagen sofort fehl | App läuft nicht, oder `server.host` / Port ist falsch |
| Navigationsschritte schlagen fehl | `processHash` in `IvyAppConfig` stimmt nicht mit Ihrer App überein — aus der `.ivp`-URL im Designer kopieren |
| Verbindung verweigert auf CI | Der Testrechner kann den App-Server nicht erreichen — Firewall / Netzwerkkonfiguration prüfen |

Für detaillierte Anfrage-/Antwort-Inspektion während der lokalen Entwicklung: `resultsTreeVisualizer()` in `IvyLoadTestRunner` einkommentieren.
