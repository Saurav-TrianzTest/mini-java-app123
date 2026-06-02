#!/usr/bin/env sh
# Minimal Maven wrapper script to invoke project-local Maven if available,
# or fall back to system Maven.

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
MVN_CMD="mvn"

if [ -x "$BASEDIR/.mvn/wrapper/maven-wrapper.jar" ]; then
  MVN_CMD="java -Dmaven.multiModuleProjectDirectory=$BASEDIR -cp \"$BASEDIR/.mvn/wrapper/maven-wrapper.jar\" org.apache.maven.wrapper.MavenWrapperMain"
fi

exec $MVN_CMD "$@"
