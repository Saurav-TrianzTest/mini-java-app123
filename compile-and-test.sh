#!/bin/bash
set -e

echo "=== Compiling and Testing Java Project ==="
cd /modernize-data/studio-data/TNT1001/APP1503/transformed-code/390/studio-workspace/validate-test-001

# Download Maven if not present
if [ ! -d "apache-maven-3.9.6" ]; then
    echo "Downloading Maven..."
    wget -q https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
    tar -xzf apache-maven-3.9.6-bin.tar.gz
    rm apache-maven-3.9.6-bin.tar.gz
fi

export MAVEN_HOME=$(pwd)/apache-maven-3.9.6
export PATH=$MAVEN_HOME/bin:$PATH

echo "Maven version:"
mvn -version

echo ""
echo "=== Step 1: Clean project ==="
mvn clean

echo ""
echo "=== Step 2: Compile main sources ==="
mvn compiler:compile

echo ""
echo "=== Step 3: Compile test sources ==="
mvn compiler:testCompile

echo ""
echo "=== Step 4: Run tests with JaCoCo ==="
mvn test jacoco:report

echo ""
echo "=== DONE ==="
echo "JaCoCo report available at: target/site/jacoco/index.html"
