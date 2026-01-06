# SSL Configuration Quick Reference

## What Changed?

Instead of bundling a `keystore.p12` file directly in the application:

### Before ❌
```
classpath:keystore.p12 (embedded in JAR)
  ↓
Spring Boot loads keystore directly
```

### After ✅
```
src/main/resources/certs/
├── certificate.pem     (separated)
└── private-key.pem     (separated)
  ↓
SslConfiguration (Spring bean)
  ↓
CertificateGenerator (BouncyCastle)
  ↓
/tmp/keystore.p12 (generated at startup)
  ↓
Spring Boot loads keystore from /tmp
```

## Key Benefits

1. **Better Security**: Certificate and key are stored separately
2. **Easier Management**: Update certs without recompiling
3. **Environment Flexibility**: Different certs per environment
4. **No Keystore Complexity**: Simple PEM format to work with

## Configuration Files

### 1. **application.properties**
Located at: `src/main/resources/application.properties`

```properties
# Enable/disable SSL
app.ssl.enabled=true

# Paths to PEM files
app.ssl.cert-path=classpath:certs/certificate.pem
app.ssl.key-path=classpath:certs/private-key.pem

# Output keystore path (will be generated here)
app.ssl.keystore-path=/tmp/keystore.p12

# Keystore password
app.ssl.keystore-password=changeit

# Spring Boot SSL settings (uses the generated keystore)
server.ssl.key-store=file:/tmp/keystore.p12
server.ssl.key-store-password=changeit
server.port=443
```

## Java Classes Added

### 1. **CertificateGenerator.java**
- Location: `src/main/java/.../system/CertificateGenerator.java`
- Purpose: Utility to generate PKCS12 keystore from PEM files
- Methods:
  - `generateKeystoreFromPem()` - Creates keystore from cert + key
  - `loadCertificate()` - Loads X509 certificate from PEM
  - `loadPrivateKey()` - Loads private key from PEM
  - `isKeystoreValid()` - Validates existing keystore

### 2. **SslConfiguration.java**
- Location: `src/main/java/.../system/SslConfiguration.java`
- Purpose: Spring configuration bean for SSL setup
- Features:
  - Listens for `ApplicationReadyEvent`
  - Automatically generates keystore on startup
  - Property-based configuration
  - Skips regeneration if keystore exists

## Certificate Files

### Locations
```
src/main/resources/certs/
├── certificate.pem     (X.509 certificate in PEM format)
└── private-key.pem     (RSA private key in PEM format)
```

### Regenerate Certificates (Development)

```bash
# Linux/Mac
./generate-certificates.sh [common-name] [validity-days]
./generate-certificates.sh localhost 365
./generate-certificates.sh myapp.example.com 730

# Windows (using Git Bash or WSL)
bash generate-certificates.sh localhost 365
```

Or manually with OpenSSL:

```bash
cd src/main/resources/certs
openssl req -x509 -newkey rsa:2048 -keyout private-key.pem -out certificate.pem \
  -days 365 -nodes -subj "/CN=localhost"
```

## Environment Variables

Override properties without modifying files:

```bash
export APP_SSL_ENABLED=true
export APP_SSL_CERT_PATH=/path/to/certificate.pem
export APP_SSL_KEY_PATH=/path/to/private-key.pem
export APP_SSL_KEYSTORE_PATH=/tmp/keystore.p12
export APP_SSL_KEYSTORE_PASSWORD=mypassword
export APP_SSL_KEY_ALIAS=my-app

mvn spring-boot:run
```

## Running the Application

### Development (Default - HTTPS)
```bash
mvn spring-boot:run
# Access: https://localhost (ignore SSL warnings for self-signed certs)
```

### Development (HTTP)
```bash
mvn spring-boot:run -Dapp.ssl.enabled=false -Dserver.port=8080
# Access: http://localhost:8080
```

### Production
```bash
# Replace certificate.pem and private-key.pem with production certs
# Then run normally:
mvn spring-boot:run
# Or package and run:
mvn clean package
java -jar target/spring-petclinic-3.5.0-SNAPSHOT.jar
```

## Troubleshooting

### Certificate not found
- Check file path in `app.ssl.cert-path`
- For classpath resources: use `classpath:certs/certificate.pem`
- For filesystem: use absolute path like `/etc/ssl/certs/certificate.pem`

### Keystore generation fails
- Verify PEM files are in correct format
- Check BouncyCastle dependencies are loaded: `mvn dependency:tree | grep bouncy`
- Check logs for detailed error: `mvn spring-boot:run 2>&1 | grep -i "ssl\|certificate"`

### Port 443 already in use
- Change port: `mvn spring-boot:run -Dserver.port=8443`
- Or add to `application.properties`: `server.port=8443`

### Certificate warnings in browser
- This is normal for self-signed certificates in development
- Click "Advanced" → "Proceed anyway" (Chrome)
- Or add exception (Firefox)
- For testing with curl: `curl -k https://localhost`

## Dependency Management

The following BouncyCastle libraries are automatically included via Maven:

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

Verify they're installed: `mvn dependency:tree | grep bouncycastle`

## File Locations Summary

| File | Purpose | Location |
|------|---------|----------|
| Certificate (PEM) | X.509 certificate | `src/main/resources/certs/certificate.pem` |
| Private Key (PEM) | RSA private key | `src/main/resources/certs/private-key.pem` |
| CertificateGenerator | Utility class | `src/main/java/.../system/CertificateGenerator.java` |
| SslConfiguration | Configuration bean | `src/main/java/.../system/SslConfiguration.java` |
| app.properties | Config properties | `src/main/resources/application.properties` |
| Generated Keystore | PKCS12 (runtime) | `/tmp/keystore.p12` |
| Documentation | Full guide | `SSL_CERTIFICATE_CONFIGURATION.md` |
| Script | Generate certs | `generate-certificates.sh` |

## Next Steps

1. **Review** the SSL_CERTIFICATE_CONFIGURATION.md for detailed documentation
2. **Update** certificate and private key files for your environment
3. **Customize** properties in application.properties as needed
4. **Test** by running: `mvn spring-boot:run`
5. **Verify** SSL is working: `curl -k https://localhost`

## Notes

- Keystore is generated in `/tmp/` (temporary) - regenerated on every app restart
- For production, consider using a writable, persistent path
- Sample certificates are self-signed and suitable only for development/testing
- Always use CA-signed certificates in production environments
