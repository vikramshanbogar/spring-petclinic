package org.springframework.samples.petclinic.system;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class KmsConfig {

	@Bean
	public KmsClient kmsClient() {
		return KmsClient.builder()
			.region(Region.AP_SOUTH_1) // change your region
			.build();
	}

}
