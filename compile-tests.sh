#!/bin/bash
# Compile Java source and test files manually

# Create output directories
mkdir -p target/classes
mkdir -p target/test-classes

# Get all dependencies
CLASSPATH="/home/studioai/.m2/repository/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"
CLASSPATH="$CLASSPATH:/home/studioai/.m2/repository/org/springframework/boot/spring-boot-starter-web/2.7.15/spring-boot-starter-web-2.7.15.jar"
CLASSPATH="$CLASSPATH:/home/studioai/.m2/repository/org/springframework/boot/spring-boot/2.7.15/spring-boot-2.7.15.jar"
CLASSPATH="$CLASSPATH:/home/studioai/.m2/repository/org/springframework/spring-core/5.3.29/spring-core-5.3.29.jar"

# Test dependencies
TEST_CP="/home/studioai/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.9.3/junit-jupiter-api-5.9.3.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/junit/jupiter/junit-jupiter-engine/5.9.3/junit-jupiter-engine-5.9.3.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/junit/platform/junit-platform-engine/1.9.3/junit-platform-engine-1.9.3.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/junit/platform/junit-platform-commons/1.9.3/junit-platform-commons-1.9.3.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/mockito/mockito-core/5.3.1/mockito-core-5.3.1.jar"
TEST_CP="$TEST_CP:/home/studioai/.m2/repository/org/mockito/mockito-junit-jupiter/5.3.1/mockito-junit-jupiter-5.3.1.jar"

echo "Attempting manual compilation without JDK..."
echo "This script requires javac compiler which is not available in JRE-only environment"
echo "ERROR: No compiler available"
exit 1
