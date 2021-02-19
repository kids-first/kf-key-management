package io.kidsfirst.core.service;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class KMSService {

  private Environment env;
  private String keyId;
  private AWSKMS kms;

  public KMSService(Environment env){
    this.env = env;
    this.keyId = this.env.getProperty("application.kms", this.env.getProperty("kms"));

    // Do we need to make this configurable via the application.yml?
    this.kms = AWSKMSClient.builder().build();
  }

  public String encrypt(String original) {

    try {
      val bufferedOriginal = StringToByteBuffer(original);
      val encryptRequest = new EncryptRequest();
      encryptRequest.withKeyId(keyId);
      encryptRequest.setPlaintext(bufferedOriginal);

      val result = kms.encrypt(encryptRequest);
      val bufferedCipher = result.getCiphertextBlob();
      return ByteBufferToString(bufferedCipher);

    } catch (UnsupportedEncodingException e){
      // Shouldn't be reachable, handle anyways
      log.error(e.getMessage(), e);
      return null;
    }

  }

  public String decrypt(String cipher) {

    try {
      val bufferedCipher = StringToByteBuffer(cipher);
      val decryptRequest = new DecryptRequest();
      decryptRequest.setCiphertextBlob(bufferedCipher);

      val result = kms.decrypt(decryptRequest);
      val bufferedOriginal = result.getPlaintext();
      return ByteBufferToString(bufferedOriginal);

    } catch (UnsupportedEncodingException e){
      // Shouldn't be reachable, handle anyways
      log.error(e.getMessage(), e);
      return null;
    }

  }

  private ByteBuffer StringToByteBuffer(String string) throws UnsupportedEncodingException {
    val bytes = string.getBytes(StandardCharsets.ISO_8859_1);
    return ByteBuffer.wrap(bytes);
  }

  private String ByteBufferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
    byte[] bytes;
    bytes = buffer.array();
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }
}
