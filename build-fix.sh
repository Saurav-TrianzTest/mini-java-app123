#!/bin/bash

# Build script with proper JAVA_HOME configuration
# This script ensures Maven uses JDK instead of JRE

export JAVA_HOME=/home/studioai/.sdkman/candidates/java/11.0.25-tem
export PATH=$JAVA_HOME/bin:$PATH

echo "Using Java from: $JAVA_HOME"
java -version
echo ""
echo "Using javac from: $(which javac)"
javac -version
echo ""

cd /modernize-data/studio-data/TNT1001/APP1522/transformed-code/400/studio-workspace/component-123

echo "Starting Maven build..."
mvn clean compile -DskipTests

BUILD_STATUS=$?

if [ $BUILD_STATUS -eq 0 ]; then
    echo ""
    echo "BUILD SUCCESS"
    exit 0
else
    echo ""
    echo "BUILD FAILED with exit code: $BUILD_STATUS"
    exit $BUILD_STATUS
fi
