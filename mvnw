#!/bin/sh
# Maven Wrapper script
# https://github.com/apache/maven-wrapper

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support
case "$(uname -s)" in
  CYGWIN* | MINGW* | MSYS*) cygwin=true ;;
  *) cygwin=false ;;
esac

# Resolve the directory of this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$SCRIPT_DIR}"
export MAVEN_PROJECTBASEDIR

MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Download maven-wrapper.jar if it doesn't exist
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    WRAPPER_URL=$(grep 'wrapperUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
    if command -v wget > /dev/null 2>&1; then
      wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v curl > /dev/null 2>&1; then
      curl -sL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    else
      echo "Error: Neither wget nor curl found. Cannot download maven-wrapper.jar"
      exit 1
    fi
  fi
fi

# Find Java
if [ -n "${JAVA_HOME-}" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="$(command -v java)" || true
fi

if [ ! -x "${JAVA_EXE-}" ]; then
  echo "Error: JAVA_HOME is not set and java is not in PATH"
  exit 1
fi

# Run Maven
exec "$JAVA_EXE" \
  ${MAVEN_OPTS-} \
  -classpath "$MAVEN_WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
