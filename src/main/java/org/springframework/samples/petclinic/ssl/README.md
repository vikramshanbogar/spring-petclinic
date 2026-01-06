# SSL Configuration Package

This package provides reusable SSL configuration for Spring Boot applications using PEM-formatted certificates.

## Overview

The SSL configuration package handles dynamic generation of PKCS12 keystores from PEM-formatted certificate and private key files. This approach allows you to:

- Store SSL certificates in source control as PEM files
- Generate keystores dynamically at application startup
- Avoid embedding binary keystores in your application
- Easily support multiple environments with different certificates

## Components

### 1. `KeystoreInitializer`
An `EnvironmentPostProcessor` that runs very early in Spring Boot's startup process (before SSL configuration is loaded). It:
- Reads PEM certificate and private key files from classpath or AWS Secrets Manager
- Generates a PKCS12 keystore
- Handles both filesystem and JAR-bundled resources
- Automatically extracts classpath resources to temporary files when running from JARs
- Supports AWS Secrets Manager for secure certificate storage

### 2. `CertificateGenerator`
A utility class that handles:
- Loading X.509 certificates from PEM files
- Loading private keys from PEM files
- Creating PKCS12 keystores from certificate and key material
- Validating existing keystores

### 3. `AWSSecretsManagerProvider`
A utility class for retrieving certificates from AWS Secrets Manager:
- Fetches secrets by name from AWS Secrets Manager
- Parses JSON secrets containing "certificate" and "privateKey" fields
- Writes credentials to temporary files for processing
- Supports AWS SDK authentication via IAM roles, environment variables, or credentials file

### 4. `SslConfiguration`
A Spring `@Configuration` class that marks the SSL configuration as active. The actual setup is done by `KeystoreInitializer`.

## Installation

### 1. Add Dependencies

Add BouncyCastle dependencies to your `pom.xml`:

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

### 2. Copy SSL Package Files

Copy the SSL package to your project:
```
src/main/java/org/springframework/samples/petclinic/ssl/
  ├── CertificateGenerator.java
  ├── KeystoreInitializer.java
  └── SslConfiguration.java
```

Update the package name to match your application structure:
```
com.example.myapp.config.ssl/
  ├── CertificateGenerator.java
  ├── KeystoreInitializer.java
  └── SslConfiguration.java
```

### 3. Register the EnvironmentPostProcessor

Create or update `src/main/resources/META-INF/spring.factories`:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=org.springframework.samples.petclinic.ssl.KeystoreInitializer
```

Update the fully qualified class name if you changed the package name.

### 4. Add Certificate and Key Files

Create `src/main/resources/certs/` directory and add:

```
src/main/resources/certs/
  ├── certificate.pem      # X.509 certificate in PEM format
  └── private-key.pem      # RSA private key in PKCS#8 PEM format
```

#### Generating Self-Signed Certificates

Using Java keytool:

```bash
# Generate keystore
keytool -genkeypair \
  -keystore temp.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -keypass changeit \
  -alias my-app \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -dname "CN=localhost,O=MyOrg,C=US"

# Export certificate
keytool -exportcert \
  -keystore temp.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -alias my-app \
  -rfc > certificate.pem

# Export private key (requires keytool with Java 15+)
# Or use OpenSSL to extract from PKCS12
openssl pkcs12 -in temp.p12 -nocerts -out private-key.pem -password pass:changeit
```

### 5. Configure application.properties

#### Option 1: Using Classpath Resources (Default)

```properties
# SSL Configuration
app.ssl.enabled=true
app.ssl.cert-path=classpath:certs/certificate.pem
app.ssl.key-path=classpath:certs/private-key.pem
app.ssl.keystore-path=target/keystore.p12
app.ssl.keystore-password=changeit
app.ssl.key-alias=my-app

# Server SSL Configuration
server.ssl.key-store=file:target/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=my-app
server.port=443
```

#### Option 2: Using AWS Secrets Manager

First, add AWS SDK dependency:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.25.0</version>
</dependency>
```

Then configure the secret in AWS Secrets Manager with JSON containing:

```json
{
  "certificate": "-----BEGIN CERTIFICATE-----\n...certificate content...\n-----END CERTIFICATE-----",
  "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...key content...\n-----END RSA PRIVATE KEY-----"
}
```

Finally, set the configuration:

```properties
# SSL Configuration - AWS Secrets Manager
app.ssl.enabled=true
app.ssl.aws-secret-name=vik/secrets-cert
app.ssl.keystore-path=target/keystore.p12
app.ssl.keystore-password=changeit
app.ssl.key-alias=my-app

# Server SSL Configuration
server.ssl.key-store=file:target/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=my-app
server.port=443
```

### AWS Secrets Manager Prerequisites

1. **AWS IAM Permissions**: Your application needs the following IAM policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "secretsmanager:GetSecretValue"
            ],
            "Resource": "arn:aws:secretsmanager:*:*:secret:vik/secrets-cert-*"
        }
    ]
}
```

2. **AWS Credentials**: The application will automatically use:
   - AWS IAM role (if running on EC2/ECS)
   - Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`)
   - AWS credentials file (`~/.aws/credentials`)
   - Assume role STS credentials

3. **Secret Format**: Store your certificate and key as a JSON secret:

```bash
aws secretsmanager create-secret \
  --name vik/secrets-cert \
  --secret-string '{
    "certificate": "-----BEGIN CERTIFICATE-----\nMIIDaz...\n-----END CERTIFICATE-----",
    "privateKey": "-----BEGIN RSA PRIVATE KEY-----\nMIIEv...\n-----END RSA PRIVATE KEY-----"
  }'
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.ssl.enabled` | `true` | Enable/disable SSL initialization |
| `app.ssl.aws-secret-name` | `` | AWS Secrets Manager secret name (e.g., "vik/secrets-cert"). If set, takes precedence over classpath resources |
| `app.ssl.cert-path` | `classpath:certs/certificate.pem` | Path to PEM certificate file (used if aws-secret-name is not set) |
| `app.ssl.key-path` | `classpath:certs/private-key.pem` | Path to PEM private key file (used if aws-secret-name is not set) |
| `app.ssl.keystore-path` | `target/keystore.p12` | Output path for generated PKCS12 keystore |
| `app.ssl.keystore-password` | `changeit` | Password for the keystore |
| `app.ssl.key-alias` | `spring-petclinic` | Alias for the certificate in the keystore |

## How It Works

1. **Early Startup Phase**: Spring Boot starts loading the environment
2. **KeystoreInitializer Activates**: As an `EnvironmentPostProcessor`, it runs before Spring creates beans
3. **Validation**: Checks if a valid keystore already exists
4. **Resource Resolution**: 
   - For filesystem resources: Uses the file directly
   - For JAR resources: Extracts to a temporary directory
5. **Certificate Loading**: Uses BouncyCastle's PEMParser to load the certificate and key
6. **Keystore Generation**: Creates a PKCS12 keystore containing the certificate and key
7. **SSL Configuration**: Spring Boot's SSL configuration picks up the generated keystore
8. **SSL Startup**: Tomcat (or other servlet container) initializes HTTPS on port 443

## Cross-Platform Path Support

The keystore path uses `target/keystore.p12` which works across:
- **Windows**: `target\keystore.p12`
- **Linux/Mac**: `target/keystore.p12`

For production deployments, consider using:
```properties
app.ssl.keystore-path=/var/lib/myapp/keystore.p12
```

## Troubleshooting

### Certificate Not Found
```
java.io.FileNotFoundException: class path resource [certs/certificate.pem] cannot be resolved
```
**Solution**: Ensure the certificate file exists at `src/main/resources/certs/certificate.pem` and is included in the built JAR.

### Invalid Certificate Format
```
org.bouncycastle.openssl.PEMException: problem parsing cert
```
**Solution**: 
- Verify the PEM file is a valid X.509 certificate
- PEM files should start with `-----BEGIN CERTIFICATE-----`
- Ensure base64 content is properly formatted

### Invalid Private Key Format
```
org.bouncycastle.openssl.PEMException: problem parsing key
```
**Solution**:
- Verify the PEM file contains a valid RSA private key
- PEM files should start with `-----BEGIN PRIVATE KEY-----` (PKCS#8 format)
- Not compatible with `-----BEGIN RSA PRIVATE KEY-----` (PKCS#1 format)

To convert PKCS#1 to PKCS#8:
```bash
openssl pkcs8 -topk8 -in private-key-pkcs1.pem -out private-key.pem -nocrypt
```

### Keystore Password Mismatch
Ensure that `app.ssl.keystore-password` and `server.ssl.key-store-password` are the same.

## Using in Other Projects

To use this SSL package in other Spring Boot projects:

1. Copy the three Java classes to your `src/main/java` directory under your package structure
2. Update the package declarations to match your project
3. Create `src/main/resources/META-INF/spring.factories` with the updated class name
4. Add BouncyCastle dependencies
5. Create PEM certificate and key files
6. Configure `application.properties` as shown above
7. Rebuild and run your application

## License

Apache License 2.0 (same as Spring Boot)

## See Also

- [Spring Boot SSL Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl)
- [BouncyCastle Documentation](https://www.bouncycastle.org/)
- [PKCS#12 Format](https://en.wikipedia.org/wiki/PKCS_12)
- [PEM Format](https://en.wikipedia.org/wiki/Privacy-Enhanced_Mail)
