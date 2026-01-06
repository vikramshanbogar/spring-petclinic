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

package org.springframework.samples.petclinic.system;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.samples.petclinic.ssl.AWSSecretsManagerProvider;

/**
 * Environment post-processor that retrieves database credentials from AWS Secrets Manager
 * and adds them to the Spring Environment.
 *
 * Configuration:
 * - app.db.aws-secret-name: The name of the secret in AWS Secrets Manager.
 *
 * The secret should contain JSON with keys: "username", "password", and optionally "url".
 */
public class AWSSecretsManagerDatabaseInitializer implements EnvironmentPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AWSSecretsManagerDatabaseInitializer.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String secretName = environment.getProperty("app.db.aws-secret-name");

		if (secretName == null || secretName.isEmpty()) {
			return;
		}

		logger.info("Initializing database credentials from AWS Secrets Manager secret: {}", secretName);

		try {
			String secretValue = AWSSecretsManagerProvider.getSecret(secretName);
			
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode secretJson = objectMapper.readTree(secretValue);
			
			Map<String, Object> secretProperties = new HashMap<>();
			
			if (secretJson.has("username")) {
				secretProperties.put("spring.datasource.username", secretJson.get("username").asText());
			}
			if (secretJson.has("password")) {
				secretProperties.put("spring.datasource.password", secretJson.get("password").asText());
			}
			if (secretJson.has("url")) {
				secretProperties.put("spring.datasource.url", secretJson.get("url").asText());
			}

			if (!secretProperties.isEmpty()) {
				// Add as first property source to take precedence
				environment.getPropertySources().addFirst(new MapPropertySource("awsSecretsManagerProperties", secretProperties));
				logger.info("Successfully loaded database credentials from AWS Secrets Manager");
			} else {
				logger.warn("Secret {} retrieved but contained no 'username', 'password', or 'url' keys", secretName);
			}

		} catch (Exception e) {
			logger.error("Failed to load database credentials from AWS Secrets Manager", e);
			throw new RuntimeException("Failed to load database credentials from AWS Secrets Manager", e);
		}
	}

}
