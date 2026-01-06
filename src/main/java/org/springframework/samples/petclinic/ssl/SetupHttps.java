package org.springframework.samples.petclinic.ssl;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility to verify HTTPS setup using the provided secret data.
 * This simulates the data being retrieved from AWS Secrets Manager.
 */
public class SetupHttps {

    public static void main(String[] args) {
        try {
            System.out.println("Starting HTTPS Setup Verification...");

            // Raw data exactly as it might come from AWS Secrets Manager JSON
            String rawPrivateKey = "-----BEGIN RSA PRIVATE KEY----- MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDuMG5ydUSOeuCk T8XH8FpEvYibdENafBbPDOGDgVo1vCeOH/mWKH99WF0MMKInVNv/p6y9Z93gLDzL WA9JWDRl3RbyhyCO8E0ayipGvx62wzD3dIjZHEwbnZzZrDDwjWoYx14XreetVym4 +ffwQJZklQMuQ5fe5gWPUZrx1ukkZfGW4jdJC/MtG/fpH8P5ok0+6o35T5HQ7V0j g56SCNSG9R4P82SH2M1tqLmG11VnSxTYKU4XcViZVgOS3KyY3prUfv4hUBvmF5XB j2ttwx/WxtwpdjNNHxBe08zJ6ucRWW1Ryios+ESp/v+FF5YkcXlK9H0bEqMmgRCm 8krFnmG7AgMBAAECgf9vLyxfQV4/JBy4loQ8UHXSoKaj6iGGm2YKQHkEad0yOCI+ ta5L6ZuZZyYICqumsHYBYMPBpjpr6jp2sSRxmDrdUdbkXy9Tidh0KOqTG6O6rGKv IDbBIfRk763H7hjqMRjVeDrz0rnSsF1Je2XBQGTzjpttfR/nbHgr7mvld+xckFMk ZD1vhhaQCWux5qByYdCXjoOvdg/KC9Qc6VrftTHn7O6f9NI4+tHLg9tp5B4cFO/9 AnWAVkoTPbU2hwHlatX3VFPM1b6rzze3VUn5h3XJ63cnn4uHJtC+QL4iL+FzKZ+l yQhHKjRAJGx/2btaLQxuVJnM6rwFrQKN4GjaxmkCgYEA8uhrccFMS43XYCXFVabR TUyVIk0eAalzbGmZVsTHAzC7pfcamd6fOJAlxgP5D+qSqoGhweHdGxszb+fB1Z8t PQ5xkEHK5gNSsbKD8Ey9LIJZ/EzGYPH9EQI1L0VRZEPkZGKkXqprl9eopTXYXEhI RUK9DjfDgE/bRSxer+Zdx7MCgYEA+wbnfiJ/Vgf4lA2kuI7H3swMWxZJq3eUd1j/ xZVsg/jHNIIOdrrKGAaRnwtw8wIvNbPUyCtN/nvtfDO0nDofMhtjj98sNd9wgof5 QsVLtqpfefHrYH4PDYMIr/huoqSpXOOPc4qyM/cVth+HkUIdoiEclj0+/ozukBLr g+IR+dkCgYEAtWKM6PL4r+qJC4jo5F6fKGOpcWW4pHEOecQY9swV9/HqDbKJsKCm sKwjKMxKrXHyd1Q5n/ezDcywCj47ZfNo6pDgiGI4yq1skLjHASesk5H69AW4lSfe W1cUs+nFL2PrJnLqXq5PjaFCk6uzlqV7Ig0F85n8aNI44FyTkDIrkTUCgYEAvfPj iJciND+j4qTQWLfylq1hTCv5Yz8Boa4/HYyMqPDADQO9XirKcpPF7cwClGC6yLBj +3SJeGDevHWD3hQ9NvVyyZPfKIUD5kJuZBBX0snppaVir4m0ak0VbdNYJMrlPd0U 2CHD5365dy80rr6RHfZgY2/FE2ohu/QVM58Ga0ECgYBFcVki7D5quJAVywC5PetC wzfx24wZz7wQYNVEm8tQOQ8yzJu4ydxFYGiyW8cY/SxDtsl92iovh5MtzLauZ5+y TA+E4qyVL7uJWLm3W0AZIYkAaiQ7pUDDLEou/kXJbI1tk4B4qQ5CBoF2ULtNO533 rSlVTElKw1WDNB1V84IqEw== -----END RSA PRIVATE KEY-----";
            String rawCertificate = "-----BEGIN CERTIFICATE----- MIIDBDCCAeygAwIBAgIJAKGXKs5p3BR9MA0GCSqGSIb3DQEBDAUAMDAxCzAJBgNV BAYTAlVTMQ0wCwYDVQQKEwRUZXN0MRIwEAYDVQQDEwlsb2NhbGhvc3QwHhcNMjYw MTA2MTEwNjQ1WhcNMjcwMTA2MTEwNjQ1WjAwMQswCQYDVQQGEwJVUzENMAsGA1UE ChMEVGVzdDESMBAGA1UEAxMJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEFAAOC AQ8AMIIBCgKCAQEA7jBucnVEjnrgpE/Fx/BaRL2Im3RDWnwWzwzhg4FaNbwnjh/5 lih/fVhdDDCiJ1Tb/6esvWfd4Cw8y1gPSVg0Zd0W8ocgjvBNGsoqRr8etsMw93SI 2RxMG52c2aww8I1qGMdeF63nrVcpuPn38ECWZJUDLkOX3uYFj1Ga8dbpJGXxluI3 SQvzLRv36R/D+aJNPuqN+U+R0O1dI4OekgjUhvUeD/Nkh9jNbai5htdVZ0sU2ClO F3FYmVYDktysmN6a1H7+IVAb5heVwY9rbcMf1sbcKXYzTR8QXtPMyernEVltUcoq LPhEqf7/hReWJHF5SvR9GxKjJoEQpvJKxZ5huwIDAQABoyEwHzAdBgNVHQ4EFgQU T1AOTafh9UIxazRZqmOg4dWrALowDQYJKoZIhvcNAQEMBQADggEBAIXeQHDiv/5P 4jdJqStjxPWbT3lsbU3MXGalIVVLtyKWpfN+lTFwNPkQLdXw0ygERgStDxcUhL7W ZhJMKbDZ5xtSLFajtZF/aRLL3FeCbnFB7P/x0SWXlC6upqBxMrTJac2GQx5cITTG hiWVj1QX5+izLtTXhlIt2e3uHZmCMbx9OnL+NpvxVxERk9rjB3E1JTrAcYCHWcU4 TBEWz7IQoWDRszGl7CHSpU37VXv6ctCR+umJLqsNzZQmyws5qoucaGMeDxOUKXag 4VVF+ZV7WVSLtSMNn96TdB6uhk8C+uetGjqc1vg7VCvvSYH0tnyh0ep6xSw+m/Zx hpip7rScpGA= -----END CERTIFICATE-----";

            // Simulate AWS provider writing to temp files
            Path keyFile = Files.createTempFile("setup-key", ".pem");
            Path certFile = Files.createTempFile("setup-cert", ".pem");

            Files.writeString(keyFile, rawPrivateKey);
            Files.writeString(certFile, rawCertificate);
            
            System.out.println("Written temp files from raw data: " + keyFile + ", " + certFile);

            String keystorePath = "target/keystore.p12";
            Files.createDirectories(Path.of("target"));

            // This now calls the improved CertificateGenerator
            CertificateGenerator.generateKeystoreFromPem(
                certFile.toAbsolutePath().toString(), 
                keyFile.toAbsolutePath().toString(), 
                keystorePath, 
                "changeit", 
                "spring-petclinic"
            );

            System.out.println("Keystore generated successfully at " + keystorePath);

            // Cleanup
            Files.delete(keyFile);
            Files.delete(certFile);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}