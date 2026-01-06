# Implementation Checklist

Use this checklist when implementing certificate-based SSL configuration in other Spring Boot projects.

## Pre-Implementation

- [ ] Review `IMPLEMENTATION_SUMMARY.md` for overview
- [ ] Review `SSL_CERTIFICATE_CONFIGURATION.md` for detailed documentation
- [ ] Review `SSL_QUICK_REFERENCE.md` for quick reference
- [ ] Understand BouncyCastle and PEM certificate formats

## Step 1: Add Dependencies

- [ ] Add BouncyCastle bcprov-jdk15on to `pom.xml`
- [ ] Add BouncyCastle bcpkix-jdk15on to `pom.xml`
- [ ] Run `mvn clean install` to verify dependencies load
- [ ] Verify with `mvn dependency:tree | grep bouncy`

## Step 2: Create Utility Classes

- [ ] Create `CertificateGenerator.java`:
  - [ ] Implement `loadCertificate()` method
  - [ ] Implement `loadPrivateKey()` method
  - [ ] Implement `generateKeystoreFromPem()` method
  - [ ] Implement `isKeystoreValid()` method
  - [ ] Add comprehensive logging
  - [ ] Add error handling

- [ ] Create `SslConfiguration.java`:
  - [ ] Add `@Configuration` annotation
  - [ ] Add property fields with `@Value` annotations
  - [ ] Implement `onApplicationReady()` method
  - [ ] Add event listener for `ApplicationReadyEvent`
  - [ ] Implement path resolution logic
  - [ ] Add comprehensive logging

## Step 3: Update Configuration

- [ ] Update `application.properties`:
  - [ ] Add `app.ssl.enabled` property
  - [ ] Add `app.ssl.cert-path` property
  - [ ] Add `app.ssl.key-path` property
  - [ ] Add `app.ssl.keystore-path` property
  - [ ] Add `app.ssl.keystore-password` property
  - [ ] Add `app.ssl.key-alias` property
  - [ ] Update `server.ssl.*` properties to point to generated keystore

## Step 4: Create Certificate Files

- [ ] Create `src/main/resources/certs/` directory
- [ ] Add `certificate.pem` (X.509 certificate)
- [ ] Add `private-key.pem` (RSA private key)
- [ ] Verify files are valid PEM format:
  - [ ] Certificate starts with `-----BEGIN CERTIFICATE-----`
  - [ ] Private key starts with `-----BEGIN` (PRIVATE KEY, RSA PRIVATE KEY, etc.)

## Step 5: Create Helper Scripts

- [ ] Create `generate-certificates.sh` for certificate generation
- [ ] Make script executable: `chmod +x generate-certificates.sh`
- [ ] Test script: `./generate-certificates.sh localhost 365`

## Step 6: Documentation

- [ ] Create `SSL_CERTIFICATE_CONFIGURATION.md` with full documentation
- [ ] Create `SSL_QUICK_REFERENCE.md` for quick reference
- [ ] Create `IMPLEMENTATION_SUMMARY.md` with summary of changes
- [ ] Update project `README.md` to mention SSL setup

## Step 7: Testing

- [ ] Build project: `mvn clean package`
- [ ] Run application: `mvn spring-boot:run`
- [ ] Verify keystore is generated:
  - [ ] Check if `/tmp/keystore.p12` exists
  - [ ] Check logs for "PKCS12 keystore successfully created"
- [ ] Test HTTPS connection:
  - [ ] `curl -k https://localhost`
  - [ ] Check certificate details: `openssl s_client -connect localhost:443`
- [ ] Test HTTP fallback (disable SSL):
  - [ ] `mvn spring-boot:run -Dapp.ssl.enabled=false -Dserver.port=8080`
  - [ ] `curl http://localhost:8080`

## Step 8: Security Configuration

- [ ] Add to `.gitignore`:
  ```
  src/main/resources/certs/private-key.pem
  /tmp/keystore.p12
  ```
- [ ] Review certificate permissions
- [ ] Set keystore file permissions: `chmod 600 /tmp/keystore.p12`
- [ ] Review password security (change default password in production)

## Step 9: CI/CD Integration

### GitHub Actions
- [ ] Add workflow for building with SSL enabled
- [ ] Configure certificate secrets in repository
- [ ] Add certificate validation step

### Docker
- [ ] Update Dockerfile to mount certificate volumes
- [ ] Document certificate mounting procedure
- [ ] Test container with mounted certificates

### Kubernetes
- [ ] Create secret for certificates
- [ ] Update deployment to use secret
- [ ] Configure volume mounts in pod spec
- [ ] Test with mounted certificates

## Step 10: Documentation Review

- [ ] Add architecture diagram to documentation
- [ ] Add deployment instructions for different environments
- [ ] Add troubleshooting section
- [ ] Add FAQ section
- [ ] Add links to external resources

## Step 11: Production Preparation

- [ ] Obtain production certificates from CA
- [ ] Test with production certificates
- [ ] Verify certificate chain is complete
- [ ] Test certificate renewal procedure
- [ ] Document certificate rotation process
- [ ] Set up monitoring for certificate expiration

## Step 12: Deployment

### Development
- [ ] Use self-signed certificates (provided)
- [ ] Use HTTP on alternate port if needed

### Staging
- [ ] Use staging certificates from CA
- [ ] Test complete SSL chain
- [ ] Load test with SSL enabled

### Production
- [ ] Use production certificates from CA
- [ ] Set secure file permissions
- [ ] Monitor certificate expiration
- [ ] Configure certificate renewal
- [ ] Document backup procedure for certificates

## Verification Checklist

### Functionality
- [ ] Application starts successfully
- [ ] Keystore is generated at startup
- [ ] HTTPS requests are handled correctly
- [ ] Certificate chain is valid
- [ ] Different environments use different certificates

### Performance
- [ ] Startup time is acceptable (< 5 seconds additional)
- [ ] Memory usage is acceptable
- [ ] No memory leaks detected

### Security
- [ ] Private keys are not in version control
- [ ] Private keys are not in JAR files
- [ ] Keystore permissions are correct
- [ ] No sensitive data in logs
- [ ] No hardcoded passwords in code

### Compatibility
- [ ] All existing tests pass
- [ ] All existing endpoints work
- [ ] Configuration works with environment variables
- [ ] Configuration works with application.properties
- [ ] Configuration works with JVM arguments

## Common Issues & Solutions

### Issue: "Certificate file not found"
- [ ] Verify `app.ssl.cert-path` in application.properties
- [ ] Verify file exists at specified location
- [ ] For classpath: ensure file is in `src/main/resources/certs/`
- [ ] Check for typos in filename

### Issue: "Invalid private key format"
- [ ] Verify private key is in PEM format
- [ ] Check file starts with `-----BEGIN`
- [ ] Verify file ends with `-----END`
- [ ] Try regenerating certificate with helper script

### Issue: "Keystore generation failed"
- [ ] Check BouncyCastle dependencies are loaded
- [ ] Check logs for detailed error message
- [ ] Verify certificate and key match
- [ ] Try with sample certificates first

### Issue: "Port 443 already in use"
- [ ] Change port in `application.properties`
- [ ] Or use command line: `-Dserver.port=8443`
- [ ] Or disable SSL for development

### Issue: "SSL handshake errors"
- [ ] Check certificate validity dates
- [ ] Verify certificate chain is complete
- [ ] Check certificate CN/SAN matches hostname
- [ ] Try with `-k` flag in curl (allow insecure)

## Rollback Plan

If issues occur and rollback is needed:

- [ ] Keep backup of original `keystore.p12` file
- [ ] Keep backup of original `application.properties`
- [ ] Document any changes made
- [ ] Have rollback commands ready
- [ ] Test rollback procedure in non-production first

### Rollback Steps
1. [ ] Restore original `application.properties`
2. [ ] Restore original `keystore.p12` to classpath
3. [ ] Remove `CertificateGenerator.java` and `SslConfiguration.java`
4. [ ] Remove BouncyCastle dependencies from pom.xml
5. [ ] Rebuild and redeploy

## Sign-Off

- [ ] Implementation complete
- [ ] All tests pass
- [ ] All documentation updated
- [ ] Code review completed
- [ ] Ready for deployment
- [ ] Deployment date: _________
- [ ] Deployed by: _________
- [ ] Verified in production: _________

## Notes

Use this space for additional notes or non-standard configurations:

```
[Your notes here]
```
