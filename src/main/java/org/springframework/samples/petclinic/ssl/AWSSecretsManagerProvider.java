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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Provider for reading secrets from AWS Secrets Manager.
 */
public class AWSSecretsManagerProvider {

	private static final Logger logger = LoggerFactory.getLogger(AWSSecretsManagerProvider.class);

	/**
	 * Retrieves the raw secret string from AWS Secrets Manager.
	 *
	 * @param secretName the name of the secret
	 * @return the secret string value
	 * @throws RuntimeException if the secret cannot be retrieved
	 */
	public static String getSecret(String secretName) {
		logger.info("Retrieving secret from AWS Secrets Manager: {}", secretName);
		try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
			GetSecretValueRequest request = GetSecretValueRequest.builder()
					.secretId(secretName)
					.build();

			GetSecretValueResponse response = client.getSecretValue(request);
			String secretValue = response.secretString();

			if (secretValue == null) {
				throw new IllegalArgumentException("Secret does not contain a string value: " + secretName);
			}
			return secretValue;
		} catch (Exception e) {
			logger.error("Failed to retrieve secret from AWS Secrets Manager: {}", secretName, e);
			throw new RuntimeException("Failed to retrieve secret from AWS Secrets Manager", e);
		}
	}

	/**
	 * Retrieves certificate and private key from AWS Secrets Manager and writes them to temporary files.
	 *
	 * @param secretName the name of the secret in AWS Secrets Manager (e.g., "vik/secrets-cert")
	 * @return an array containing [certificatePath, privateKeyPath]
	 * @throws Exception if an error occurs retrieving or writing the files
	 */
	public static String[] getCertificateAndKeyFromAWS(String secretName) throws Exception {
		String secretValue = getSecret(secretName);

		// Parse JSON secret
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode secretJson = objectMapper.readTree(secretValue);

		String certificate = secretJson.has("certificate") ? secretJson.get("certificate").asText() : null;
		String privateKey = secretJson.has("privateKey") ? secretJson.get("privateKey").asText() : null;

		if (certificate == null || certificate.isEmpty()) {
			throw new IllegalArgumentException("Secret does not contain 'certificate' field");
		}
		if (privateKey == null || privateKey.isEmpty()) {
			throw new IllegalArgumentException("Secret does not contain 'privateKey' field");
		}

		// Create temporary files
		Path certFile = Files.createTempFile("petclinic-cert-", ".pem");
		Path keyFile = Files.createTempFile("petclinic-key-", ".pem");

		// Write to temporary files
		Files.write(certFile, certificate.getBytes());
		Files.write(keyFile, privateKey.getBytes());

		// Mark for deletion on exit
		certFile.toFile().deleteOnExit();
		keyFile.toFile().deleteOnExit();

		logger.info("Certificate written to: {}", certFile.toAbsolutePath());
		logger.info("Private key written to: {}", keyFile.toAbsolutePath());

		return new String[] { certFile.toAbsolutePath().toString(), keyFile.toAbsolutePath().toString() };
	}

}
