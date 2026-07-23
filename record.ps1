# record.ps1 - record a browser session against your Ivy app and print the values
# you paste into the CUSTOMIZE FOR YOUR APP block of PortalWalkthroughLoadTest.
#
# One command, jbang only. A browser opens; walk your scenario, then CLOSE it.
#
#   .\record.ps1 http://my-host:8080/          # record (URL filter auto-derived)
#   .\record.ps1 http://my-host:8080/ myrec    # custom working dir (default: recording)
#
# Note: use your machine's hostname or LAN IP rather than localhost where possible,
# and HTTP (not HTTPS) for a local Ivy Designer, so the proxy can see the traffic.
param(
  [Parameter(Mandatory = $true)] [string] $Url,
  [string] $WorkDir = "recording"
)

$scriptDir = $PSScriptRoot

# JMeter's recorder loads ALL its function plugins on the first captured request;
# the JavaScript/JEXL functions need engines the jmeter-java-dsl-cli jar does not
# bundle, so we add them here (otherwise: NoClassDefFoundError org/mozilla/...).
$deps = "org.mozilla:rhino:1.7.14,org.apache.commons:commons-jexl3:3.2.1,org.apache.commons:commons-jexl:2.1.1"

# Record only traffic to the app under test (drops Chrome/Google background noise).
# --url-includes is a regex matched against the scheme-stripped URL; an unescaped
# "." still matches the literal dot, so host:port.* is enough (and matches the docs).
$hostport = $Url -replace '^[a-zA-Z]+://', '' -replace '/.*$', ''
$filter = "$hostport.*"

New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null

Write-Host "* A browser will open. Walk through your scenario (open, log in, click the"
Write-Host "  pages you want to test, log out), then CLOSE the browser window to finish."
Write-Host "  (first run downloads dependencies - may take a minute)"
Write-Host ""

# The recorder usually throws a 'TimeoutException ... PT30S' while finishing against
# chatty apps (Portal push/notification polling). That is harmless here: the proxy
# log (.jtl) is always written, and that is what we read. Output goes to a log file
# so the scary-but-irrelevant stack trace does not clutter the console.
#
# --log-filtered-requests is CRITICAL: --url-includes still focuses the test plan,
# but without this flag every non-matching request (login redirects, pages served as
# localhost vs 127.0.0.1, external hosts) is dropped from the .jtl silently - so parts
# of your journey would vanish. With it, the .jtl logs EVERYTHING and the extractor
# picks out the app-relevant bits.
$log = Join-Path $WorkDir "recorder.log"
& jbang --deps $deps us.abstracta.jmeter:jmeter-java-dsl-cli:2.2 recorder `
  "--url-includes=$filter" `
  "--log-filtered-requests" `
  "--workdir=$WorkDir" `
  $Url *> $log

Write-Host "* Recording captured. Reading your template values..."
Write-Host ""
& jbang (Join-Path $scriptDir "RecordIds.java") $WorkDir
Write-Host ""
Write-Host "(Raw recorder output, if you need it: $log)"
