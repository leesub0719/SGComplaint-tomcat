#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TOMCAT_HOME="${TOMCAT_HOME:-/opt/tomcat}"
TOMCAT_SERVICE="${TOMCAT_SERVICE:-tomcat10}"
APP_CONTEXT="${APP_CONTEXT:-sgcomplaint}"
WAR_PATH="${WAR_PATH:-$PROJECT_ROOT/build/libs/sgcomplaint.war}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/$APP_CONTEXT/actuator/health}"
SKIP_BUILD="${SKIP_BUILD:-false}"

if [[ ! "$APP_CONTEXT" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "APP_CONTEXT에 허용되지 않는 문자가 있습니다: $APP_CONTEXT" >&2
  exit 2
fi

WEBAPPS_DIR="$TOMCAT_HOME/webapps"
TARGET_WAR="$WEBAPPS_DIR/$APP_CONTEXT.war"
EXPLODED_DIR="$WEBAPPS_DIR/$APP_CONTEXT"

[[ -x "$TOMCAT_HOME/bin/catalina.sh" ]] || { echo "Tomcat 10.1 경로가 아닙니다: $TOMCAT_HOME" >&2; exit 2; }
tomcat_version="$("$TOMCAT_HOME/bin/catalina.sh" version 2>&1)" || {
  echo "Tomcat 버전을 확인할 수 없습니다: $TOMCAT_HOME" >&2
  exit 2
}
grep -q 'Apache Tomcat/10\.1\.' <<< "$tomcat_version" || {
  echo "Tomcat 10.1.x만 지원합니다: $TOMCAT_HOME" >&2
  exit 2
}
[[ -d "$WEBAPPS_DIR" ]] || { echo "webapps 디렉터리가 없습니다: $WEBAPPS_DIR" >&2; exit 2; }

if [[ "$SKIP_BUILD" != "true" ]]; then
  "$PROJECT_ROOT/gradlew" --no-daemon clean test bootWar
fi
[[ -f "$WAR_PATH" ]] || { echo "WAR 파일이 없습니다: $WAR_PATH" >&2; exit 2; }

sudo systemctl stop "$TOMCAT_SERVICE"

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_dir="$PROJECT_ROOT/backup/tomcat/$timestamp"
mkdir -p "$backup_dir"

if [[ -f "$TARGET_WAR" ]]; then
  sudo cp -p "$TARGET_WAR" "$backup_dir/$APP_CONTEXT.war"
fi
if [[ -d "$EXPLODED_DIR" ]]; then
  sudo tar -C "$WEBAPPS_DIR" -czf "$backup_dir/$APP_CONTEXT-exploded.tar.gz" "$APP_CONTEXT"

  expected="$(realpath -m "$WEBAPPS_DIR/$APP_CONTEXT")"
  case "$expected" in
    "$(realpath "$WEBAPPS_DIR")"/*) sudo rm -rf -- "$expected" ;;
    *) echo "안전하지 않은 삭제 경로입니다: $expected" >&2; exit 3 ;;
  esac
fi

staged="$WEBAPPS_DIR/$APP_CONTEXT.war.deploying"
sudo install -m 0640 "$WAR_PATH" "$staged"
sudo mv -f "$staged" "$TARGET_WAR"
sudo systemctl start "$TOMCAT_SERVICE"

for _ in $(seq 1 30); do
  if curl --fail --silent --show-error "$HEALTH_URL" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    echo "배포 및 health 확인 완료: $HEALTH_URL"
    echo "이전 버전 백업: $backup_dir"
    exit 0
  fi
  sleep 2
done

echo "health 확인 실패. journalctl -u $TOMCAT_SERVICE 및 $TOMCAT_HOME/logs를 확인하세요." >&2
echo "롤백 파일: $backup_dir/$APP_CONTEXT.war" >&2
exit 1
