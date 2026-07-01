#!/usr/bin/env bash
# record.sh - record a browser session against your Ivy app and print the values
# you paste into the CUSTOMIZE FOR YOUR APP block of PortalWalkthroughLoadTest.
#
# One command, jbang only. A browser opens; walk your scenario, then CLOSE it.
#
#   ./record.sh http://my-host:8080/          # record (URL filter auto-derived)
#   ./record.sh http://my-host:8080/ myrec    # custom working dir (default: recording)
#
# Note: use your machine's hostname or LAN IP rather than localhost where possible,
# and HTTP (not HTTPS) for a local Ivy Designer, so the proxy can see the traffic.
set -uo pipefail

URL="${1:-}"
if [ -z "$URL" ]; then
  echo "Usage: ./record.sh <app-url> [workdir]"
  echo "   e.g. ./record.sh http://my-host:8080/"
  exit 1
fi
WORKDIR="${2:-recording}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# JMeter's recorder loads ALL its function plugins on the first captured request;
# the JavaScript/JEXL functions need engines the jmeter-java-dsl-cli jar does not
# bundle, so we add them here (otherwise: NoClassDefFoundError org/mozilla/...).
DEPS="org.mozilla:rhino:1.7.14,org.apache.commons:commons-jexl3:3.2.1,org.apache.commons:commons-jexl:2.1.1"

# Record only traffic to the app under test (drops Chrome/Google background noise).
# --url-includes is a regex matched against the scheme-stripped URL; an unescaped
# "." still matches the literal dot, so host:port.* is enough (and matches the docs).
HOSTPORT="$(printf '%s' "$URL" | sed -E 's#^[a-zA-Z]+://##; s#/.*$##')"
FILTER="${HOSTPORT}.*"

mkdir -p "$WORKDIR"

echo "* A browser will open. Walk through your scenario (open, log in, click the"
echo "  pages you want to test, log out), then CLOSE the browser window to finish."
echo "  (first run downloads dependencies - may take a minute)"
echo

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
jbang --deps "$DEPS" us.abstracta.jmeter:jmeter-java-dsl-cli:2.2 recorder \
  --url-includes="$FILTER" \
  --log-filtered-requests \
  --workdir="$WORKDIR" \
  "$URL" > "$WORKDIR/recorder.log" 2>&1 || true

echo "* Recording captured. Reading your template values..."
echo
jbang "$SCRIPT_DIR/RecordIds.java" "$WORKDIR"
echo
echo "(Raw recorder output, if you need it: $WORKDIR/recorder.log)"
