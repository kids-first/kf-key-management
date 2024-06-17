package io.kidsfirst.core.service;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class StringCompressService {

    public static String compress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
            return out.toString(StandardCharsets.ISO_8859_1);
        } catch(IOException e) {
            log.error("Error during string compress", e);
            return str;
        }
    }

    public static String decompress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        try {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes(StandardCharsets.ISO_8859_1)));
            BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "ISO_8859_1"));
            StringBuilder outStr = new StringBuilder();
            String line;
            while ((line=bf.readLine())!=null) {
                outStr.append(line);
            }
            return outStr.toString();
        } catch (IOException e) {
            log.error("Error during string decompress", e);
            return str;
        }
    }
}
