# SSL Certificate Generation Configuration

This document explains how the Spring PetClinic application now handles SSL/TLS certificates using separate PEM-formatted certificate and private key files with dynamic PKCS12 keystore generation.

## Overview

Instead of bundling a pre-built `keystore.p12` file, the application now:

1. **Stores certificates separately**: Certificate and private key are stored as PEM files
2. **Generates keystore dynamically**: On application startup, the keystore is generated from PEM files using BouncyCastle
3. **Improves security**: Certificate and private key are separate, making them easier to manage and rotate
4. **Simplifies deployment**: No need to pre-generate keystores for different environments

## Architecture

### Files Structure

```
src/main/resources/
├── certs/
│   ├── certificate.pem      # X.509 certificate (PEM format)
│   └── private-key.pem      # Private key (PEM format)
└── application.properties   # Configuration pointing to PEM files
```

### Key Components

#### 1. **CertificateGenerator.java**
Utility class that handles:
- Loading PEM-formatted certificates and private keys using BouncyCastle
- Creating PKCS12 keystores from the loaded certificate and key
- Validating existing keystores

**Location**: `src/main/java/org/springframework/samples/petclinic/system/CertificateGenerator.java`

#### 2. **SslConfiguration.java**
Spring configuration class that:
- Listens for application startup events
- Automatically generates the PKCS12 keystore from PEM files
- Skips generation if a valid keystore already exists
- Provides flexible property-based configuration

**Location**: `src/main/java/org/springframework/samples/petclinic/system/SslConfiguration.java`

#### 3. **application.properties**
Configuration properties for SSL:
```properties
# SSL is enabled by default
app.ssl.enabled=true

# Paths to PEM files (classpath: prefix works for resources)
app.ssl.cert-path=classpath:certs/certificate.pem
app.ssl.key-path=classpath:certs/private-key.pem

# Where to save the generated PKCS12 keystore
app.ssl.keystore-path=/tmp/keystore.p12

# Keystore credentials
app.ssl.keystore-password=changeit
app.ssl.key-alias=spring-petclinic

# Spring Boot SSL configuration (uses the generated keystore)
server.ssl.key-store=file:/tmp/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=spring-petclinic
server.port=443
```

## How It Works

### Application Startup Flow

1. **Spring Application Starts**
2. **ApplicationReadyEvent is triggered**
3. **SslConfiguration.onApplicationReady() executes**
4. **Checks if keystore already exists** (avoids regeneration)
5. **Loads PEM certificate and private key** using BouncyCastle
6. **Creates PKCS12 keystore** from the loaded key material
7. **Spring Boot initializes SSL** using the generated keystore
8. **Application is ready for HTTPS requests**

### Configuration Properties

All properties are customizable via `application.properties` or environment variables:

| Property | Default | Description |
|----------|---------|-------------|
| `app.ssl.enabled` | `true` | Enable/disable SSL configuration |
| `app.ssl.cert-path` | `classpath:certs/certificate.pem` | Path to certificate file |
| `app.ssl.key-path` | `classpath:certs/private-key.pem` | Path to private key file |
| `app.ssl.keystore-path` | (empty) | Output path for generated keystore |
| `app.ssl.keystore-password` | `changeit` | Password for the keystore |
| `app.ssl.key-alias` | `spring-petclinic` | Alias for the key entry |

## Usage

### Using Default Configuration

The default configuration works out of the box with the sample PEM files included:

```bash
mvn spring-boot:run
```

The application will:
- Read `src/main/resources/certs/certificate.pem`
- Read `src/main/resources/certs/private-key.pem`
- Generate keystore at `/tmp/keystore.p12`
- Start on HTTPS port 443

### Custom Certificate and Key

To use your own certificate and key:

1. **Place your files** in the classpath or filesystem:
   ```
   src/main/resources/certs/my-certificate.pem
   src/main/resources/certs/my-private-key.pem
   ```

2. **Update application.properties**:
   ```properties
   app.ssl.cert-path=classpath:certs/my-certificate.pem
   app.ssl.key-path=classpath:certs/my-private-key.pem
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

### Using Environment Variables

Set environment variables instead of modifying properties:

```bash
export APP_SSL_CERT_PATH=/etc/ssl/certs/certificate.pem
export APP_SSL_KEY_PATH=/etc/ssl/private/private-key.pem
export APP_SSL_KEYSTORE_PATH=/var/lib/keystore.p12
mvn spring-boot:run
```

### Disabling SSL

To disable SSL during development:

```properties
app.ssl.enabled=false
server.port=8080
```

Or via environment variable:

```bash
export APP_SSL_ENABLED=false
mvn spring-boot:run
```

## Certificate Format Requirements

### Certificate (PEM)
- Format: **PEM** (Privacy Enhanced Mail)
- Format: **X.509** v3
- Must contain `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----` markers

Example:
```
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIUAKGHg+...
...
...
-----END CERTIFICATE-----
```

### Private Key (PEM)
- Format: **PEM** 
- Can be in one of these formats:
  - PKCS#8: `-----BEGIN PRIVATE KEY-----`
  - PKCS#1 (RSA): `-----BEGIN RSA PRIVATE KEY-----`
  - OpenSSL: `-----BEGIN PRIVATE KEY-----`

Example:
```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQE...
...
...
-----END PRIVATE KEY-----
```

## Generating Self-Signed Certificates

For development/testing, generate a self-signed certificate:

```bash
# Using OpenSSL
openssl req -x509 -newkey rsa:2048 \
  -keyout private-key.pem \
  -out certificate.pem \
  -days 365 -nodes \
  -subj "/CN=localhost"
```

Or using Java keytool (to convert to PEM):

```bash
# Generate keystore
keytool -genkey -alias spring-petclinic -keyalg RSA -keystore keystore.p12 \
  -storepass changeit -validity 365

# Export certificate
keytool -export -alias spring-petclinic -keystore keystore.p12 \
  -storepass changeit -file certificate.crt

# Convert to PEM
openssl x509 -inform DER -in certificate.crt -out certificate.pem

# Extract private key (requires keytool-to-pem conversion)
```

## Dependencies

The following BouncyCastle libraries are required and automatically managed by Maven:

```xml
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcprov-jdk15on</artifactId>
  <version>1.70</version>
</dependency>
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcpkix-jdk15on</artifactId>
  <version>1.70</version>
</dependency>
```

## Troubleshooting

### Issue: "Certificate file not found"

**Solution**: Ensure the path is correct and the file exists:
```properties
# For classpath resources, use classpath: prefix
app.ssl.cert-path=classpath:certs/certificate.pem

# For filesystem paths, use absolute or relative paths
app.ssl.cert-path=/etc/ssl/certs/certificate.pem
```

### Issue: "Invalid private key format"

**Solution**: Ensure the private key is in PEM format:
```bash
# Check the header
head -1 private-key.pem
# Should output: -----BEGIN PRIVATE KEY----- or -----BEGIN RSA PRIVATE KEY-----
```

### Issue: "Keystore generation failed"

**Solution**: Check the logs for detailed error messages:
```bash
mvn spring-boot:run 2>&1 | grep -i "ssl\|certificate\|keystore"
```

### Issue: "Port 443 already in use"

**Solution**: Change the port in application.properties:
```properties
server.port=8443
```

### Issue: Certificate mismatch or validation errors

**Solution**: Ensure certificate and private key match:
```bash
# Extract public key from certificate
openssl x509 -in certificate.pem -pubkey -noout > pub.pem

# Extract public key from private key
openssl pkey -in private-key.pem -pubout > pub2.pem

# Compare
diff pub.pem pub2.pem
```

## Security Considerations

1. **Protect Private Keys**: Never commit private keys to version control
2. **Use .gitignore**: Add PEM files to `.gitignore`:
   ```
   src/main/resources/certs/private-key.pem
   /tmp/keystore.p12
   ```
3. **Keystore Password**: Use strong passwords in production
4. **Certificate Validity**: Monitor certificate expiration dates
5. **Key Rotation**: Implement regular key rotation procedures

## Environment-Specific Configuration

Create environment-specific property files:

```
application-dev.properties      # Development SSL settings
application-prod.properties     # Production SSL settings
application-test.properties     # Test SSL settings
```

Then run with:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## Integration with Container Deployments

### Docker

Mount certificate and key files as volumes:

```dockerfile
# Dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/spring-petclinic-3.5.0-SNAPSHOT.jar app.jar
EXPOSE 443
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Run with mounted certs
docker run -v /etc/ssl/certs:/etc/ssl/certs \
           -v /etc/ssl/private:/etc/ssl/private \
           -e APP_SSL_CERT_PATH=/etc/ssl/certs/certificate.pem \
           -e APP_SSL_KEY_PATH=/etc/ssl/private/private-key.pem \
           spring-petclinic
```

### Kubernetes

Use secrets for certificates:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ssl-certs
type: Opaque
data:
  certificate.pem: <base64-encoded-cert>
  private-key.pem: <base64-encoded-key>
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: petclinic
spec:
  template:
    spec:
      containers:
      - name: petclinic
        image: spring-petclinic:latest
        volumeMounts:
        - name: certs
          mountPath: /etc/ssl/certs
          readOnly: true
        env:
        - name: APP_SSL_CERT_PATH
          value: /etc/ssl/certs/certificate.pem
        - name: APP_SSL_KEY_PATH
          value: /etc/ssl/certs/private-key.pem
      volumes:
      - name: certs
        secret:
          secretName: ssl-certs
```

## Testing

### Testing HTTPS Connection

```bash
# Using curl (allow self-signed certificates)
curl -k --cacert certificate.pem https://localhost

# Using openssl
openssl s_client -connect localhost:443 -cert certificate.pem
```

### Unit Tests

The `CertificateGenerator` class can be tested independently:

```java
@Test
public void testKeystoreGeneration() throws Exception {
    String certPath = "classpath:certs/certificate.pem";
    String keyPath = "classpath:certs/private-key.pem";
    String keystorePath = "/tmp/test-keystore.p12";
    
    CertificateGenerator.generateKeystoreFromPem(
        certPath, keyPath, keystorePath, "test-password", "test-alias");
    
    assertTrue(CertificateGenerator.isKeystoreValid(keystorePath));
}
```

## Additional Resources

- [BouncyCastle Documentation](https://www.bouncycastle.org/)
- [Java Keystore Documentation](https://docs.oracle.com/javase/tutorial/security/pkcs11/)
- [Spring Boot SSL/TLS Configuration](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.security.ssl)
- [OpenSSL Certificate Generation](https://www.openssl.org/docs/manmaster/man1/openssl-req.html)
