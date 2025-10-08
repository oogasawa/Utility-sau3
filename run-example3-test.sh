#!/bin/bash

# Example 3 Test Runner Script
# This script helps run the Example 3 remote deployment tests

set -e

echo "=================================================="
echo "  Example 3 Remote Deployment Test Runner"
echo "=================================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java is not installed${NC}"
    exit 1
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "${GREEN}✓ Java: $JAVA_VERSION${NC}"
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven is not installed${NC}"
    exit 1
else
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    echo -e "${GREEN}✓ Maven: $MVN_VERSION${NC}"
fi

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    exit 1
else
    if ! docker info &> /dev/null; then
        echo -e "${RED}✗ Docker daemon is not running${NC}"
        echo "  Please start Docker: sudo systemctl start docker"
        exit 1
    else
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | tr -d ',')
        echo -e "${GREEN}✓ Docker: $DOCKER_VERSION${NC}"
    fi
fi

# Check Yarn
if ! command -v yarn &> /dev/null; then
    echo -e "${YELLOW}⚠ Yarn is not installed (tests will be skipped)${NC}"
    echo "  Install with: npm install -g yarn"
else
    YARN_VERSION=$(yarn --version)
    echo -e "${GREEN}✓ Yarn: $YARN_VERSION${NC}"
fi

echo ""
echo "All prerequisites met!"
echo ""

# Ask user which test to run
echo "Select test to run:"
echo "1) Example 3 format test (IP address + /var/www/html)"
echo "2) User public_html test"
echo "3) baseUrl with \$USER variable test"
echo "4) SSH container accessibility test"
echo "5) All remote deployment tests"
echo "6) All tests (including local deployment tests)"
read -p "Enter choice [1-6]: " choice

case $choice in
    1)
        echo ""
        echo "Running Example 3 format test..."
        mvn test -Dtest=SauDeployRemoteTest#testRemoteDeploymentExample3Format
        ;;
    2)
        echo ""
        echo "Running user public_html test..."
        mvn test -Dtest=SauDeployRemoteTest#testRemoteDeploymentToUserHome
        ;;
    3)
        echo ""
        echo "Running baseUrl with \$USER variable test..."
        mvn test -Dtest=SauDeployRemoteTest#testBaseUrlWithUserVariable
        ;;
    4)
        echo ""
        echo "Running SSH container accessibility test..."
        mvn test -Dtest=SauDeployRemoteTest#testSSHContainerAccessibility
        ;;
    5)
        echo ""
        echo "Running all remote deployment tests..."
        mvn test -Dtest=SauDeployRemoteTest
        ;;
    6)
        echo ""
        echo "Running all tests..."
        mvn clean test
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}=================================================="
echo "  Test execution completed!"
echo -e "==================================================${NC}"
