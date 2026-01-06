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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * SSL Configuration class for Spring applications using PEM-based SSL certificates.
 *
 * This configuration class serves as a marker for enabling SSL support via PEM files.
 * The actual keystore generation is handled by KeystoreInitializer which runs early
 * in the Spring Boot startup process, before SSL configuration is loaded.
 *
 * To use this SSL package in your project:
 *
 * 1. Add BouncyCastle dependencies to pom.xml:
 *    - org.bouncycastle:bcprov-jdk15on:1.70
 *    - org.bouncycastle:bcpkix-jdk15on:1.70
 *
 * 2. Create PEM certificate and private key files:
 *    - src/main/resources/certs/certificate.pem
 *    - src/main/resources/certs/private-key.pem
 *
 * 3. Configure application.properties:
 *    app.ssl.enabled=true
 *    app.ssl.cert-path=classpath:certs/certificate.pem
 *    app.ssl.key-path=classpath:certs/private-key.pem
 *    app.ssl.keystore-path=target/keystore.p12
 *    app.ssl.keystore-password=changeit
 *    app.ssl.key-alias=your-app-name
 *    server.ssl.key-store=file:target/keystore.p12
 *    server.ssl.key-store-password=changeit
 *    server.ssl.key-store-type=PKCS12
 *    server.ssl.key-alias=your-app-name
 *    server.port=443
 */
@Configuration
public class SslConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SslConfiguration.class);

	public SslConfiguration() {
		logger.info("SSL Configuration initialized with PEM-based certificate support");
	}

}
