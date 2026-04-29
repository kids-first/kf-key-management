package io.kidsfirst.core.service;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class StringCompressService {

    public static String compress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.close();
            return out.toString(StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            log.error("Error during string compress", e);
            return str;
        }
    }

    public static String decompress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes(StandardCharsets.ISO_8859_1)))) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error during string decompress", e);
            return str;
        }
    }
}
