package io.kidsfirst.keys.core.utils;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import lombok.val;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class KMSUtils {

  private static final String keyId = System.getenv("kms");
  private static final AWSKMS kms = AWSKMSClient.builder().build();

  public static String encrypt(String original) {

    try {
      val bufferedOriginal = StringToByteBuffer(original);
      val encryptRequest = new EncryptRequest();
      encryptRequest.withKeyId(keyId);
      encryptRequest.setPlaintext(bufferedOriginal);

      val result = kms.encrypt(encryptRequest);
      val bufferedCipher = result.getCiphertextBlob();
      return ByteBufferToString(bufferedCipher);

    } catch (UnsupportedEncodingException e){
      // TODO: shouldn't be reachable, handle anyways

      return null;
    }

  }

  public static String decrypt(String cipher) {

    try {
      val bufferedCipher = StringToByteBuffer(cipher);
      val decryptRequest = new DecryptRequest();
      decryptRequest.setCiphertextBlob(bufferedCipher);

      val result = kms.decrypt(decryptRequest);
      val bufferedOriginal = result.getPlaintext();
      return ByteBufferToString(bufferedOriginal);

    } catch (UnsupportedEncodingException e){
      // TODO: shouldn't be reachable, handle anyways

      return null;
    }

  }

  private static ByteBuffer StringToByteBuffer(String string) throws UnsupportedEncodingException {
    val bytes = string.getBytes(StandardCharsets.ISO_8859_1);
    return ByteBuffer.wrap(bytes);
  }

  private static String ByteBufferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
    byte[] bytes;
    bytes = buffer.array();
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }
}
