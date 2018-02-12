package io.kidsfirst.keys.core.utils;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class KMSUtils {

  private static final String keyId = System.getenv("kms");
  private static final AWSKMS kms = AWSKMSClient.builder().build();

  public static String encrypt(String original) {

    try {
      ByteBuffer bufferedOriginal = StringToByteBuffer(original);
      EncryptRequest encryptRequest = new EncryptRequest();
      encryptRequest.withKeyId(keyId);
      encryptRequest.setPlaintext(bufferedOriginal);

      EncryptResult result = kms.encrypt(encryptRequest);
      ByteBuffer bufferedCipher = result.getCiphertextBlob();
      return ByteBufferToString(bufferedCipher);

    } catch (UnsupportedEncodingException e){
      // TODO: shouldn't be reachable, handle anyways

      return null;
    }

  }

  public static String decrypt(String cipher) {

    try {
      ByteBuffer bufferedCipher = StringToByteBuffer(cipher);
      DecryptRequest decryptRequest = new DecryptRequest();
      decryptRequest.setCiphertextBlob(bufferedCipher);

      DecryptResult result = kms.decrypt(decryptRequest);
      ByteBuffer bufferedOriginal = result.getPlaintext();
      return ByteBufferToString(bufferedOriginal);

    } catch (UnsupportedEncodingException e){
      // TODO: shouldn't be reachable, handle anyways

      return null;
    }

  }

  private static ByteBuffer StringToByteBuffer(String string) throws UnsupportedEncodingException {
    byte[] bytes = string.getBytes(StandardCharsets.ISO_8859_1);
    return ByteBuffer.wrap(bytes);
  }

  private static String ByteBufferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
    byte[] bytes;
    bytes = buffer.array();
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }
}
