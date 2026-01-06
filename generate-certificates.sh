#!/bin/bash
# Certificate Generation Helper Script
# This script generates self-signed certificates for testing and development

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/src/main/resources/certs"

# Configuration
CERT_NAME="certificate"
KEY_NAME="private-key"
COMMON_NAME="${1:-localhost}"
VALIDITY_DAYS="${2:-365}"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Spring PetClinic Certificate Generator ===${NC}\n"

# Create certs directory if it doesn't exist
mkdir -p "$CERT_DIR"

# Check if OpenSSL is installed
if ! command -v openssl &> /dev/null; then
    echo "Error: OpenSSL is not installed. Please install OpenSSL to generate certificates."
    exit 1
fi

echo "Certificate Configuration:"
echo "  Common Name (CN): $COMMON_NAME"
echo "  Validity: $VALIDITY_DAYS days"
echo "  Output Directory: $CERT_DIR"
echo ""

# Generate self-signed certificate and private key
echo -e "${BLUE}Generating self-signed certificate and private key...${NC}"

openssl req -x509 \
    -newkey rsa:2048 \
    -keyout "$CERT_DIR/$KEY_NAME.pem" \
    -out "$CERT_DIR/$CERT_NAME.pem" \
    -days "$VALIDITY_DAYS" \
    -nodes \
    -subj "/C=US/ST=California/L=San Francisco/O=Spring Samples/CN=$COMMON_NAME"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Certificate generated successfully!${NC}\n"
    
    echo "Generated files:"
    echo "  Certificate: $CERT_DIR/$CERT_NAME.pem"
    echo "  Private Key: $CERT_DIR/$KEY_NAME.pem"
    echo ""
    
    # Display certificate info
    echo -e "${BLUE}Certificate Information:${NC}"
    openssl x509 -in "$CERT_DIR/$CERT_NAME.pem" -noout -subject -dates -issuer
    echo ""
    
    # Create symlink for production usage
    echo -e "${BLUE}Next steps:${NC}"
    echo "1. The certificate and key are ready for development/testing"
    echo "2. Run: mvn spring-boot:run"
    echo "3. Access: https://localhost/ (ignore SSL warnings for self-signed certs)"
    echo ""
    echo "For production certificates:"
    echo "  - Obtain certificates from a trusted Certificate Authority (CA)"
    echo "  - Replace the generated files with production certificates"
    echo "  - No code changes needed - the app will use the new files automatically"
else
    echo -e "${RED}✗ Failed to generate certificate${NC}"
    exit 1
fi
