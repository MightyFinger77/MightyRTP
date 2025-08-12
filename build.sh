#!/bin/bash

echo "Building MightyRTP Plugin..."
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "Maven found, building plugin..."
echo

# Clean and package
mvn clean package

if [ $? -ne 0 ]; then
    echo
    echo "Build failed! Check the error messages above."
    exit 1
fi

echo
echo "Build successful! Plugin JAR is located in the target/ folder"
echo
echo "Files created:"
ls -la target/*.jar
echo
