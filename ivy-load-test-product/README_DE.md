# JMeter Java DSL Performance Test

Dieses Projekt stellt Demo-Performance-Tests als Referenz für Ihr Axon Ivy Projekt bereit und verwendet **JMeter Java DSL**, einen modernen Java-basierten Ansatz für Performance-Tests. Es bietet eine Fluent-API zur programmatischen Erstellung von JMeter-Testplänen und ersetzt XML-basierte Testpläne durch reinen Java-Code.

**Vorteile:**
- **Typsicherheit**: Validierung der Testpläne zur Kompilierzeit
- **IDE-Unterstützung**: Vollständige Debugging-Funktionen
- **Wartbarkeit**: Einfaches Refactoring und Code-Wiederverwendung
- **Versionskontrolle**: Reiner Java-Code statt XML-Dateien
- **Integration**: Nahtlose Integration mit JUnit und Test-Frameworks

## Demo

### DemoTest

Ein einfacher Smoke-Test zur Überprüfung der externen Website-Verfügbarkeit:
- Sendet HTTP-GET-Anfragen an `axonivy.com` und `market.axonivy.com`
- Validiert HTTP-200-Antwortcodes und erwartete Seiteninhalte
- Generiert JTL-Ergebnisdateien und HTML-Berichte

### PerformancePortalTest

Ein vollständiger Portal-Durchlauftest, der eine echte Benutzersitzung simuliert:
1. **Anmeldung** — Authentifizierung mit Zugangsdaten aus einer CSV-Datei
2. **Portal-Startseite** — Navigation zur Startseite
3. **Prozesse** — Öffnen der Prozessliste
4. **Aufgabenliste** — Navigation zum Aufgabenlisten-Dashboard
5. **Fallliste** — Navigation zum Falllisten-Dashboard
6. **Abmeldung** — Beenden der Sitzung

Dieser Test validiert Antwortcodes bei jedem Schritt, extrahiert dynamische Werte (ViewState, Weiterleitungs-URLs) per Regex und gibt Ergebnisse als JTL und HTML aus.

### PerformancePortalTestReviewInGui

Derselbe Portal-Durchlauf wie oben, öffnet jedoch die **JMeter-GUI** (`resultsTreeVisualizer`) zur visuellen Fehlersuche und Analyse von Anfragen/Antworten während der lokalen Entwicklung.

## Setup

### Projektstruktur
```
<ihr_projekt_anwendung>/
├── src_test/com/axonivy/
│   └── <ihr_projekt_anwendung>Test.java
├── resources/
│   ├── test.properties
│   └── <dateiname>.csv
└── target/
    └── jtls/
```

### Schnellstart
1. Repository klonen
2. Einfachen Demo-Test ausführen:
```bash
mvn clean test -Dtest=DemoTest
```
3. Für `PerformancePortalTest`:
   - Code mit Axon Ivy Designer öffnen
   - Portal vom Axon Ivy Market installieren (Version muss mit Designer kompatibel sein)
   - Benutzer-Zugangsdaten in `one_user.csv` aktualisieren
   - Ausführen: `mvn clean test -Dtest=PerformancePortalTest`

### Konfiguration

Konfigurieren Sie Ihren Test über `resources/test.properties`

### Properties-Konfiguration

#### Server-Eigenschaften
```properties
# Name des Sicherheitssystems
# Abhängig von der Servereinrichtung
# In lokaler Umgebung leer lassen
security.system.name=

# Anwendungsname
application.name=designer

# Projektname
project.name=<ihr_projektname>

# Server-Host
server.host=localhost
```

#### CSV-Datendateien
```properties
##### CSV-Datei für Benutzer
one_user.csv=resources/<dateiname>.csv
```

### CSV-Datenkonfiguration

CSV-Dateien enthalten Benutzer-Zugangsdaten im Format: `benutzername,passwort`

Beispiel (`users.csv`):
```csv
benutzer1,passwortFuerBenutzer1
```

CSV Data Set Konfiguration im Code:
```java
csvDataSet(csvFilePath)
  .variableNames("username,password")  // Spaltennamen definieren
  .delimiter(",")                      // CSV-Trennzeichen
  .ignoreFirstLine(false)              // Auf true setzen wenn CSV Header hat
```

### Sicherheitsmanagement für Zugangsdaten

Ihre Zugangsdaten-Dateien enthalten sensible Informationen und sollten **niemals** in die Versionskontrolle eingecheckt werden.

> **Hinweis:** Dies ist nur ein Beispiel, wie Dateien in einem Projekt verwendet werden können. Es gibt mehrere Möglichkeiten, Zugangsdaten zu verwalten; andere Lösungen sind möglich.

**Jenkins Secret Files**
1. Gehen Sie zu Jenkins → Jenkins verwalten → Zugangsdaten verwalten
2. Fügen Sie Zugangsdaten vom Typ "Secret file" für jede CSV-Datei hinzu
3. In Ihrer Jenkinsfile:
```groovy
pipeline {
  agent any
  stages {
    stage('Setup Credentials') {
      steps {
        script {
          withCredentials([
            file(credentialsId: 'your_credentials', variable: 'YOUR_CREDENTIALS_CSV'),
          ]) {
            sh '''
              cp "$YOUR_CREDENTIALS_CSV" "$<pfad_zur_csv_datei>"
            '''
          }
        }
      }
    }
    stage('Test') {
      steps {
        bat 'mvn test'
      }
    }
  }
}
```

**Sicherheits-Best-Practices**
1. **Niemals echte Zugangsdaten** in die Versionskontrolle einchecken
2. **Vorlagendateien verwenden** um das erwartete Format zu dokumentieren
3. **Zugriff beschränken** auf Zugangsdaten-Dateien auf dem Jenkins-Server
4. **Zugangsdaten regelmäßig rotieren**
5. **Prinzip der minimalen Berechtigung** für Testkonten verwenden

## HTTP-Sampler-Beispiele

### Einfacher HTTP-Sampler
```java
httpSampler("ProjectStart",
  "/${__P(security.system.name)}/${__P(application.name)}/pro/${__P(project.name)}/1549F58C18A6C562/DefaultApplicationHomePage.ivp")
  .method("GET")
```

### HTTP-Sampler mit Parametern
```java
httpSampler("Login", "${url}")
  .method("POST")
  .param("javax.faces.partial.ajax", "true")
  .param("javax.faces.source", "login-form:login-command")
  .param("login:login-form:username", "${username}")
  .param("login:login-form:password", "${password}")
  .param("javax.faces.ViewState", "${viewState}")
```

## Werte und Variablen übergeben

### Properties aus test.properties verwenden
Verwenden Sie die `${__P(property.name)}` Syntax um Properties zu referenzieren:
```java
.host("${__P(server.host)}") // Liest den server.host Wert aus der Datei
```

### Variablen aus CSV-Daten verwenden
Referenzieren Sie CSV-Spaltennamen als Variablen:
```java
.param("login:login-form:username", "${username}")  // Aus csvDataSet variableNames
.param("login:login-form:password", "${password}")  // Aus csvDataSet variableNames
```

### Extrahierte Variablen verwenden
Durch Regex-Extraktoren extrahierte Variablen können in nachfolgenden Anfragen verwendet werden:
```java
httpSampler("Login", "${url}")  // Verwendet extrahierte 'url' Variable
  .param("javax.faces.ViewState", "${viewState}")  // Verwendet extrahierte 'viewState' Variable
```

## Reguläre-Ausdruck-Extraktoren

Extraktoren erfassen Werte aus HTTP-Antworten zur Verwendung in nachfolgenden Anfragen:

### Einfacher Regex-Extraktor
```java
.children(
  regexExtractor("url", "action=\"([^\"]+)\""),  // Formular-Action-URL extrahieren
  regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") ")  // ViewState extrahieren
)
```

### Weiterleitungs-URL-Extraktor
```java
.children(
  regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">")  // Weiterleitungs-URL aus Antwort extrahieren
)
```

### Häufige Regex-Muster
```java
// URLs aus Action-Attributen extrahieren
regexExtractor("url", "action=\"([^\"]+)\"")

// ViewState aus JSF-Seiten extrahieren
regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") ")

// Weiterleitungs-URLs aus XML-Antworten extrahieren
regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">")
```

## Antwort-Assertions

Antwort-Assertions validieren, dass HTTP-Anfragen die erwarteten Ergebnisse liefern:

### Antwortcode-Assertion
```java
.children(
  responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
)
```

### Antwortinhalt-Assertion
```java
.children(
  responseAssertion().fieldToTest(TargetField.RESPONSE_DATA).containsSubstrings("Success")
)
```

### Verfügbare Zielfelder
- `TargetField.RESPONSE_CODE`: HTTP-Statuscode (200, 404, 500, etc.)
- `TargetField.RESPONSE_DATA`: Antwortinhalt
- `TargetField.RESPONSE_HEADERS`: HTTP-Antwort-Header
- `TargetField.RESPONSE_MESSAGE`: HTTP-Antwortnachricht

### Assertion-Methoden
- `.equalsToStrings(value)`: Exakte Übereinstimmung
- `.containsSubstrings(value)`: Enthält Teilzeichenkette
- `.matchesRegex(pattern)`: Entspricht regulärem Ausdruck
- `.notContainsSubstrings(value)`: Enthält Teilzeichenkette nicht

## Testausführungs-Konfiguration

### Thread-Gruppen-Konfiguration
```java
threadGroup("test_name")
  .rampTo(numberOfUsers, Duration.ofSeconds(rampUpPeriod))
  .holdIterating(1)
```

### HTTP-Standardwerte
```java
httpDefaults()
  .host("${__P(server.host)}")  // Standard-Host für alle Anfragen
  .port(8081) // Standard-Port für alle Anfragen
```

### HTTP-Header
```java
httpHeaders()
  .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
  .header("Accept-Encoding", "gzip, deflate, br")
  .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
```

### Cookie-Verwaltung
```java
httpCookies()  // Aktiviert automatisches Cookie-Handling
```

## Berichte und Ergebnisse

### JTL Writer (Rohergebnisse)
```java
jtlWriter(jtlDirName, testName + ".jtl")  // Speichert Rohtestergebnisse
```

### Results Tree Visualizer (zum Debuggen)
```java
resultsTreeVisualizer()  // Nur für lokales Debugging einkommentieren
```

## Fehlerbehebung

### Häufige Probleme
1. **401/403-Fehler**: Benutzer-Zugangsdaten in CSV-Dateien überprüfen
2. **ViewState-Fehler**: Sicherstellen, dass der ViewState-Extraktions-Regex korrekt ist
3. **Timeout-Probleme**: Antwortzeit-Erwartungen in den Properties anpassen
4. **Verbindungsfehler**: Server-Host und Port-Konfiguration überprüfen

### Debug-Modus
`resultsTreeVisualizer()` einkommentieren für detaillierte Anfrage-/Antwort-Inspektion während der lokalen Entwicklung. (siehe `PerformancePortalTest`)