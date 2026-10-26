#!/bin/sh
# Dev loop of the Java services: compile, run with Spring Boot DevTools, and compile
# again on every change under src/ or gen/java.
#
# DevTools only restarts the app when target/classes/.reloadtrigger changes
# (SPRING_DEVTOOLS_RESTART_TRIGGER_FILE in compose.yaml), and this script touches it
# after each successful compile, so a half-finished build never restarts the app.
set -eu

./mvnw -B -ntp -q compile

# If the app ever stops (e.g. it failed to start), start it again.
(
  while :; do
    ./mvnw -B -ntp spring-boot:run || true
    echo "java-dev: the app stopped; starting it again in 3 seconds"
    sleep 3
  done
) &

# watchexec runs the command through sh, so it's one string.
exec watchexec --watch src --watch ../../gen/java --exts java,yml,yaml,properties \
  --debounce 1s --postpone --on-busy-update queue -- \
  './mvnw -B -ntp -q -o compile && touch target/classes/.reloadtrigger'
