#!/bin/bash
docker run --rm \
  -v "$(pwd)":/workspace \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn clean test-compile
