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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for certificate generation and management.
 * Generates PKCS12 keystores from PEM-formatted certificate and private key files.
 */
public class CertificateGenerator {

	private static final Logger logger = LoggerFactory.getLogger(CertificateGenerator.class);

	/**
	 * Creates a PKCS12 keystore from separate certificate and private key files.
	 *
	 * @param certPath path to the PEM-formatted certificate file
	 * @param keyPath path to the PEM-formatted private key file
	 * @param keystorePath path where the PKCS12 keystore will be created
	 * @param keystorePassword password for the keystore
	 * @param alias alias for the key entry in the keystore
	 * @throws Exception if an error occurs during keystore creation
	 */
	public static void generateKeystoreFromPem(String certPath, String keyPath, String keystorePath,
			String keystorePassword, String alias) throws Exception {
		try {
			// Load certificate
			X509Certificate certificate = loadCertificate(certPath);
			logger.info("Certificate loaded successfully from: {}", certPath);

			// Load private key
			PrivateKey privateKey = loadPrivateKey(keyPath);
			logger.info("Private key loaded successfully from: {}", keyPath);

			// Create keystore
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(null, keystorePassword.toCharArray());

			// Add certificate and private key to keystore
			keystore.setKeyEntry(alias, privateKey, keystorePassword.toCharArray(),
					new Certificate[] { certificate });
			logger.info("Certificate and private key added to keystore with alias: {}", alias);

			// Write keystore to file
			try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
				keystore.store(fos, keystorePassword.toCharArray());
				logger.info("PKCS12 keystore successfully created at: {}", keystorePath);
			}
		} catch (Exception e) {
			logger.error("Error generating keystore from PEM files", e);
			throw e;
		}
	}

	/**
	 * Loads an X.509 certificate from a PEM-formatted file.
	 *
	 * @param certPath path to the PEM certificate file
	 * @return X509Certificate object
	 * @throws Exception if an error occurs reading the file or parsing the certificate
	 */
	private static X509Certificate loadCertificate(String certPath) throws Exception {
		String certContent = Files.readString(Path.of(certPath));
		// Clean up potential whitespace issues from AWS JSON
		if (certContent.contains("BEGIN CERTIFICATE")) {
			String header = "-----BEGIN CERTIFICATE-----";
			String footer = "-----END CERTIFICATE-----";
			int start = certContent.indexOf(header);
			int end = certContent.indexOf(footer);
			if (start != -1 && end != -1) {
				String body = certContent.substring(start + header.length(), end).replaceAll("\\s+", "");
				StringBuilder formattedBody = new StringBuilder();
				for (int i = 0; i < body.length(); i += 64) {
					formattedBody.append(body, i, Math.min(i + 64, body.length())).append("\n");
				}
				certContent = header + "\n" + formattedBody.toString() + footer;
			}
		}

		try (Reader reader = new StringReader(certContent); PEMParser parser = new PEMParser(reader)) {
			Object obj = parser.readObject();

			if (obj instanceof X509CertificateHolder) {
				X509CertificateHolder certHolder = (X509CertificateHolder) obj;
				return new JcaX509CertificateConverter().getCertificate(certHolder);
			}
			else {
				throw new IllegalArgumentException(
						"Invalid certificate format. Expected X509CertificateHolder. Got: "
								+ (obj != null ? obj.getClass().getName() : "null"));
			}
		}
	}

	/**
	 * Loads a private key from a PEM-formatted file.
	 * Supports both PKCS#1 (-----BEGIN RSA PRIVATE KEY-----) and 
	 * PKCS#8 (-----BEGIN PRIVATE KEY-----) formats.
	 *
	 * @param keyPath path to the PEM private key file
	 * @return PrivateKey object
	 * @throws Exception if an error occurs reading the file or parsing the key
	 */
	private static PrivateKey loadPrivateKey(String keyPath) throws Exception {
		String keyContent = Files.readString(Path.of(keyPath));

		// Clean up potential whitespace issues from AWS JSON (similar to loadCertificate)
		if (keyContent.contains("PRIVATE KEY")) {
			String headerVal = null;
			if (keyContent.contains("BEGIN RSA PRIVATE KEY")) {
				headerVal = "RSA PRIVATE KEY";
			} else if (keyContent.contains("BEGIN PRIVATE KEY")) {
				headerVal = "PRIVATE KEY";
			}

			if (headerVal != null) {
				String header = "-----BEGIN " + headerVal + "-----";
				String footer = "-----END " + headerVal + "-----";
				int start = keyContent.indexOf(header);
				int end = keyContent.indexOf(footer);
				if (start != -1 && end != -1) {
					String body = keyContent.substring(start + header.length(), end).replaceAll("\\s+", "");
					StringBuilder formattedBody = new StringBuilder();
					for (int i = 0; i < body.length(); i += 64) {
						formattedBody.append(body, i, Math.min(i + 64, body.length())).append("\n");
					}
					keyContent = header + "\n" + formattedBody.toString() + footer;
				}
			}
		}

		// Check if it's a PKCS#8 key mislabeled as PKCS#1
		if (keyContent.contains("-----BEGIN RSA PRIVATE KEY-----")) {
			String body = keyContent.replace("-----BEGIN RSA PRIVATE KEY-----", "")
				.replace("-----END RSA PRIVATE KEY-----", "")
				.replaceAll("\\s+", "");

			// PKCS#8 keys usually start with MIIEv or similar and are longer than PKCS#1 for the same bit length
			// But a better check is to try parsing it as PKCS#8 if PKCS#1 fails, or vice versa.
			// Here we use a heuristic: if we can't parse it with the original header, we try the other.
		}

		try (Reader reader = new StringReader(keyContent); PEMParser parser = new PEMParser(reader)) {
			Object obj = parser.readObject();

			if (obj == null) {
				throw new IllegalArgumentException("Could not read PEM object from " + keyPath);
			}

			if (obj instanceof PrivateKeyInfo) {
				// PKCS#8 format (-----BEGIN PRIVATE KEY-----)
				PrivateKeyInfo keyInfo = (PrivateKeyInfo) obj;
				return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
			}
			else if (obj instanceof RSAPrivateCrtKey) {
				// PKCS#1 format (-----BEGIN RSA PRIVATE KEY-----)
				RSAPrivateCrtKey rsaKey = (RSAPrivateCrtKey) obj;
				RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(rsaKey.getModulus(), rsaKey.getPublicExponent(),
						rsaKey.getPrivateExponent(), rsaKey.getPrimeP(), rsaKey.getPrimeQ(), rsaKey.getPrimeExponentP(),
						rsaKey.getPrimeExponentQ(), rsaKey.getCrtCoefficient());
				return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
			}
			else {
				// If first attempt failed or returned unexpected type, try forcing PKCS#8 if it was labeled RSA PRIVATE KEY
				if (keyContent.contains("BEGIN RSA PRIVATE KEY")) {
					String forcedPkcs8 = keyContent.replace("RSA PRIVATE KEY", "PRIVATE KEY");
					try (Reader reader2 = new StringReader(forcedPkcs8); PEMParser parser2 = new PEMParser(reader2)) {
						Object obj2 = parser2.readObject();
						if (obj2 instanceof PrivateKeyInfo) {
							return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj2);
						}
					}
				}
				throw new IllegalArgumentException(
						"Invalid private key format. Expected PKCS#1 (RSA PRIVATE KEY) or PKCS#8 (PRIVATE KEY) format. Got: "
								+ obj.getClass().getName());
			}
		}
		catch (Exception e) {
			// If it failed with "malformed sequence", it might be the mislabeled PKCS#8 issue
			if (e.getMessage() != null && e.getMessage().contains("malformed sequence")
					&& keyContent.contains("BEGIN RSA PRIVATE KEY")) {
				String forcedPkcs8 = keyContent.replace("RSA PRIVATE KEY", "PRIVATE KEY");
				try (Reader reader2 = new StringReader(forcedPkcs8); PEMParser parser2 = new PEMParser(reader2)) {
					Object obj2 = parser2.readObject();
					if (obj2 instanceof PrivateKeyInfo) {
						return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj2);
					}
				}
			}
			throw e;
		}
	}

	/**
	 * Checks if a keystore file exists and is valid.
	 *
	 * @param keystorePath path to the keystore file
	 * @return true if the keystore exists and can be read, false otherwise
	 */
	public static boolean isKeystoreValid(String keystorePath) {
		try {
			Path path = Path.of(keystorePath);
			return Files.exists(path) && Files.isReadable(path);
		} catch (Exception e) {
			logger.debug("Keystore validation failed for path: {}", keystorePath, e);
			return false;
		}
	}

}
