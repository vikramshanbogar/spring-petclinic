/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Environment post-processor that generates the PKCS12 keystore from PEM files
 * very early in the Spring Boot startup process, before SSL configuration is loaded.
 *
 * This initializer runs during the environment setup phase and handles:
 * - Loading PEM-formatted certificate and private key files from classpath resources
 * - Retrieving certificate and key from AWS Secrets Manager
 * - Generating a PKCS12 keystore from these files
 * - Extracting classpath resources to temporary locations when running from JARs
 *
 * Configuration properties:
 * - app.ssl.enabled: Enable/disable SSL initialization (default: true)
 * - app.ssl.aws-secret-name: AWS Secrets Manager secret name (e.g., "vik/secrets-cert")
 * - app.ssl.cert-path: Path to PEM certificate file (default: classpath:certs/certificate.pem)
 * - app.ssl.key-path: Path to PEM private key file (default: classpath:certs/private-key.pem)
 * - app.ssl.keystore-path: Output path for PKCS12 keystore (default: target/keystore.p12)
 * - app.ssl.keystore-password: Keystore password (default: changeit)
 * - app.ssl.key-alias: Certificate alias in keystore (default: spring-petclinic)
 *
 * Note: If app.ssl.aws-secret-name is configured, it takes precedence over classpath resources.
 */
public class KeystoreInitializer implements EnvironmentPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(KeystoreInitializer.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		boolean sslEnabled = environment.getProperty("app.ssl.enabled", Boolean.class, true);
		if (!sslEnabled) {
			logger.info("SSL is disabled, skipping keystore generation");
			return;
		}

		String keystorePath = environment.getProperty("app.ssl.keystore-path", "");
		if (keystorePath == null || keystorePath.isEmpty()) {
			logger.warn("Keystore path not configured (app.ssl.keystore-path), skipping keystore generation");
			return;
		}

		try {
			// Check if keystore already exists
			if (CertificateGenerator.isKeystoreValid(keystorePath)) {
				logger.info("Valid keystore already exists at: {}", keystorePath);
				return;
			}

			logger.info("Generating PKCS12 keystore from PEM files...");

			String certPath;
			String keyPath;

			// Check if AWS Secrets Manager is configured
			String awsSecretName = environment.getProperty("app.ssl.aws-secret-name", "");
			if (awsSecretName != null && !awsSecretName.isEmpty()) {
				logger.info("Using AWS Secrets Manager to retrieve certificate and key");
				String[] paths = AWSSecretsManagerProvider.getCertificateAndKeyFromAWS(awsSecretName);
				certPath = paths[0];
				keyPath = paths[1];
			} else {
				// Use classpath resources as fallback
				certPath = environment.getProperty("app.ssl.cert-path", "classpath:certs/certificate.pem");
				keyPath = environment.getProperty("app.ssl.key-path", "classpath:certs/private-key.pem");

				// Resolve resource paths
				ResourceLoader resourceLoader = new PathMatchingResourcePatternResolver();
				certPath = resolvePath(certPath, resourceLoader);
				keyPath = resolvePath(keyPath, resourceLoader);
			}

			String keystorePassword = environment.getProperty("app.ssl.keystore-password", "changeit");
			String keyAlias = environment.getProperty("app.ssl.key-alias", "spring-petclinic");

			logger.debug("Certificate path: {}", certPath);
			logger.debug("Private key path: {}", keyPath);

			// Generate keystore
			CertificateGenerator.generateKeystoreFromPem(certPath, keyPath, keystorePath,
					keystorePassword, keyAlias);

			logger.info("PKCS12 keystore successfully generated at: {}", keystorePath);

		} catch (Exception e) {
			logger.error("Failed to generate keystore from PEM files", e);
			throw new RuntimeException("SSL Configuration Error: Failed to generate keystore from PEM files", e);
		}
	}

	/**
	 * Resolves resource paths, handling classpath: prefix.
	 * For classpath resources inside JARs, extracts them to a temporary location.
	 *
	 * @param path the path to resolve
	 * @param resourceLoader the resource loader
	 * @return the resolved file path
	 * @throws IOException if resource resolution fails
	 */
	private String resolvePath(String path, ResourceLoader resourceLoader) throws IOException {
		if (path.startsWith("classpath:")) {
			Resource resource = resourceLoader.getResource(path);
			if (!resource.exists()) {
				throw new IllegalArgumentException("Resource not found: " + path);
			}
			
			try {
				// Try to get the file directly (works for filesystem resources)
				return resource.getFile().getAbsolutePath();
			} catch (IOException e) {
				// Resource is inside a JAR, extract it to a temp location
				logger.debug("Resource is inside JAR, extracting to temp location: {}", path);
				Path tempDir = Files.createTempDirectory("petclinic-certs-");
				tempDir.toFile().deleteOnExit();
				
				String filename = resource.getFilename();
				if (filename == null) {
					filename = "cert-" + System.nanoTime();
				}
				
				Path tempFile = tempDir.resolve(filename);
				try (InputStream is = resource.getInputStream()) {
					Files.copy(is, tempFile);
				}
				tempFile.toFile().deleteOnExit();
				logger.debug("Extracted {} to {}", path, tempFile.toAbsolutePath());
				return tempFile.toAbsolutePath().toString();
			}
		}
		return path;
	}

}
