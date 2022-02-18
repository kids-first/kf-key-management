package io.kidsfirst.core.service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KMSService {

    final AwsCrypto crypto;
    final KmsMasterKeyProvider keyProvider;

    public KMSService(@Value("${application.kms}") String keyId) {

        this.crypto = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
                .build();

        this.keyProvider = KmsMasterKeyProvider.builder().buildStrict(keyId);

    }

    public String encrypt(String original) {
        val result = crypto.encryptData(keyProvider, original.getBytes());
        return new String(result.getResult());
    }

    public String decrypt(String cipher) {
        val result = crypto.decryptData(keyProvider, cipher.getBytes());
        return new String(result.getResult());

    }


}
