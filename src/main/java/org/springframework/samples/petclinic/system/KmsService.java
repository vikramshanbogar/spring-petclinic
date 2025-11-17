package org.springframework.samples.petclinic.system;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;

@Service
public class KmsService {

	private final KmsClient kmsClient;

	public KmsService(KmsClient kmsClient) {
		this.kmsClient = kmsClient;
	}

	public String decrypt(String encryptedBase64) {
		byte[] cipherBytes = java.util.Base64.getDecoder().decode(encryptedBase64);

		DecryptRequest req = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(cipherBytes)).build();

		DecryptResponse resp = kmsClient.decrypt(req);
		return resp.plaintext().asUtf8String();
	}

}
