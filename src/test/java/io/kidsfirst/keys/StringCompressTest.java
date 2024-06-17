package io.kidsfirst.keys;

import org.junit.Assert;
import org.junit.Test;

import static io.kidsfirst.core.service.StringCompressService.compress;
import static io.kidsfirst.core.service.StringCompressService.decompress;

public class StringCompressTest {

    @Test
    public void testCompressDecompressString() {
        String accessToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImZlbmNlX2tleV9rZXktMDEiLCJ0eXAiOiJKV1QifQ.eyJwdXIiOiJpZCIsInN1YiI6Ijk1IiwiaXNzIjoiaHR0cHM6Ly9kYXRhLmtpZHNmaXJzdGRyYy5vcmcvdXNlciIsImlhdCI6MTcxODM4NTgzOCwiZXhwIjoxNzE4Mzg3MDM4LCJqdGkiOiI5OTM4MzZmMC1iNTZkLTQyMzEtYWZkYS1kMTQwYWVkYmIyZmMiLCJhdXRoX3RpbWUiOjE3MTgzODU4MzgsImF6cCI6IlYzVFBQRERMMlQxODB1UFRHeTBiWklEMTE0S1BzZDBqY3ppMERveFYiLCJzY29wZSI6WyJvcGVuaWQiLCJ1c2VyIiwiZGF0YSJdLCJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IkFEQU1SRVNOSUNLIiwiaXNfYWRtaW4iOmZhbHNlLCJlbWFpbCI6InJlc25pY2tAY2hvcC5lZHUiLCJkaXNwbGF5X25hbWUiOiIiLCJwaG9uZV9udW1iZXIiOiIiLCJ0YWdzIjp7ImRiZ2FwX3JvbGUiOiJEb3dubG9hZGVyIiwicGkiOiJZaXJhbiBHdW8ifSwicHJvamVjdHMiOnsiU0RfQkhKWEJEUUsiOlsicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jNCI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTEzOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMyI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTIyOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQxMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTEzOC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMyI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE3OC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jNSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTY4My5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQyMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQzNi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTExMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTk5Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTk5Ny5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTczOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTcxNC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjEzMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE2Mi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE3Mi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE2MS5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTg0Ni5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sIlNEX05NVlY4QTFZLmM5OTkiOlsicmVhZC1zdG9yYWdlIl0sIlNEXzdOUTkxNTFKLmM5OTkiOlsicmVhZC1zdG9yYWdlIl0sIlNEX05NVlY4QTFZIjpbInJlYWQtc3RvcmFnZSJdLCJTRF83TlE5MTUxSiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfMVA0MVo3ODIiOlsicmVhZC1zdG9yYWdlIl0sIlNEXzhZOTlRWkpKIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9EWVBNRUhIRiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfNDZTSzU1QTMiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1BSRUFTQTdTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF85UFlaQUhIRSI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfREswS1JXSzgiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1pGR0RHNVlTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZR1ZBMEUxQyI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfRFpUQjVIUlIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1IwRVBSU0dTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZTlNTQVBIRSI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfUk04QUZXMFIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1cwVjk2NVhaIjpbInJlYWQtc3RvcmFnZSJdLCJTRF80NlJSOVpSNiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfNkZQWUpRQlIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0I4WDNDMU1YIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9aWEpGRk1FRiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfRFo0R1BRWDYiOlsicmVhZC1zdG9yYWdlIl0sInBoczAwMTc4NS5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTgwNi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTgwNi5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE4Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjI3Ni5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjMzMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjMzMC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sIlNEXzBUWVZZMVRXIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9WVFRTSFdWNCI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfSldTM1YyNEQiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1A0NDVBQ0hWIjpbInJlYWQtc3RvcmFnZSJdLCJTRF8yQ0VLUTA1ViI6WyJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMzMwLmM5OTkiOlsicmVhZCIsInJlYWQtc3RvcmFnZSJdLCJwaHMwMDExMzguYzk5OSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMDQ2NS5jOTk5IjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAwNDY3LmM5OTkiOlsicmVhZCIsInJlYWQtc3RvcmFnZSJdLCJwaHMwMDIxODcuYzk5OSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjI3Ni5jOTk5IjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwiU0RfUEVUN1E2RjIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0FROUtWTjVQIjpbInJlYWQtc3RvcmFnZSJdLCJTRF82NTA2NFAyWiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfWjZNV0QzSDAiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0ZZQ1I3OFcwIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZNlZSRzZNRCI6WyJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMTczLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMTc0LmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNTkxLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNTkwLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNjI3LmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXX19fSwiYXVkIjpbIlYzVFBQRERMMlQxODB1UFRHeTBiWklEMTE0S1BzZDBqY3ppMERveFYiLCJodHRwczovL2RhdGEua2lkc2ZpcnN0ZHJjLm9yZy91c2VyIl0sImF0X2hhc2giOiJXaHZCQW15QkI3MEM5QjFseFhTVk93In0.pGhEez8ohIkdooXbeDEwvlwlUjL4KrQyZkr0S5JRklJukMgkEqPho6WTptjqeu4CmrMy4ZMwJh4u6DIYFbNdHYP22N0yuo_FhuuEBytYNS-3Gh8TKuIvZf5Y0qB2ENtUhl0ovQOjCgQ1iBeHrCJWwzmH2kcOMt5jqgyaQsvM3FsJMWPOggDellaR5_QgEq1bWf-ZM5P6toX54ine2SRlztHzohNdWOYRcB0zVcvARQQS2ZW-OrNIwkhfawOypYophiEAs4AUwdKq37F_jyuwd4YG3DQPd758wjVPPdML4d9A8iv_Ls8O4AjUd86FOxM4WJcJdp8CUKOx-qgGTuUtNQ";

        String compressed = compress(accessToken);
        String decompressed = decompress(compressed);

        Assert.assertEquals(accessToken, decompressed);
    }

    @Test
    public void testCompressNullString() {
        Assert.assertNull(compress(null));
    }

    @Test
    public void testCompressEmptyString() {
        String accessToken = "";

        String compressed = compress(accessToken);

        Assert.assertEquals(accessToken, compressed);
    }

    @Test
    public void testDecompressNullString() {
        Assert.assertNull(decompress(null));
    }

    @Test
    public void testDecompressEmptyString() {
        String compressed = "";

        String decompressed = decompress(compressed);

        Assert.assertEquals(compressed, decompressed);
    }

    @Test
    public void testDecompressErrorShouldReturnInputString() {
        String accessToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImZlbmNlX2tleV9rZXktMDEiLCJ0eXAiOiJKV1QifQ.eyJwdXIiOiJpZCIsInN1YiI6Ijk1IiwiaXNzIjoiaHR0cHM6Ly9kYXRhLmtpZHNmaXJzdGRyYy5vcmcvdXNlciIsImlhdCI6MTcxODM4NTgzOCwiZXhwIjoxNzE4Mzg3MDM4LCJqdGkiOiI5OTM4MzZmMC1iNTZkLTQyMzEtYWZkYS1kMTQwYWVkYmIyZmMiLCJhdXRoX3RpbWUiOjE3MTgzODU4MzgsImF6cCI6IlYzVFBQRERMMlQxODB1UFRHeTBiWklEMTE0S1BzZDBqY3ppMERveFYiLCJzY29wZSI6WyJvcGVuaWQiLCJ1c2VyIiwiZGF0YSJdLCJjb250ZXh0Ijp7InVzZXIiOnsibmFtZSI6IkFEQU1SRVNOSUNLIiwiaXNfYWRtaW4iOmZhbHNlLCJlbWFpbCI6InJlc25pY2tAY2hvcC5lZHUiLCJkaXNwbGF5X25hbWUiOiIiLCJwaG9uZV9udW1iZXIiOiIiLCJ0YWdzIjp7ImRiZ2FwX3JvbGUiOiJEb3dubG9hZGVyIiwicGkiOiJZaXJhbiBHdW8ifSwicHJvamVjdHMiOnsiU0RfQkhKWEJEUUsiOlsicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jNCI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTEzOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jMyI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTIyOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQxMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTEzOC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTI0Ny5jMyI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE3OC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTE2OC5jNSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTY4My5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQyMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTQzNi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTExMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTk5Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTk5Ny5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTczOC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTcxNC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjEzMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE2Mi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE3Mi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE2MS5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTg0Ni5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sIlNEX05NVlY4QTFZLmM5OTkiOlsicmVhZC1zdG9yYWdlIl0sIlNEXzdOUTkxNTFKLmM5OTkiOlsicmVhZC1zdG9yYWdlIl0sIlNEX05NVlY4QTFZIjpbInJlYWQtc3RvcmFnZSJdLCJTRF83TlE5MTUxSiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfMVA0MVo3ODIiOlsicmVhZC1zdG9yYWdlIl0sIlNEXzhZOTlRWkpKIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9EWVBNRUhIRiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfNDZTSzU1QTMiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1BSRUFTQTdTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF85UFlaQUhIRSI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfREswS1JXSzgiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1pGR0RHNVlTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZR1ZBMEUxQyI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfRFpUQjVIUlIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1IwRVBSU0dTIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZTlNTQVBIRSI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfUk04QUZXMFIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1cwVjk2NVhaIjpbInJlYWQtc3RvcmFnZSJdLCJTRF80NlJSOVpSNiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfNkZQWUpRQlIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0I4WDNDMU1YIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9aWEpGRk1FRiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfRFo0R1BRWDYiOlsicmVhZC1zdG9yYWdlIl0sInBoczAwMTc4NS5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTgwNi5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMTgwNi5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjE4Ny5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjI3Ni5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjMzMC5jMSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjMzMC5jMiI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sIlNEXzBUWVZZMVRXIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9WVFRTSFdWNCI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfSldTM1YyNEQiOlsicmVhZC1zdG9yYWdlIl0sIlNEX1A0NDVBQ0hWIjpbInJlYWQtc3RvcmFnZSJdLCJTRF8yQ0VLUTA1ViI6WyJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMzMwLmM5OTkiOlsicmVhZCIsInJlYWQtc3RvcmFnZSJdLCJwaHMwMDExMzguYzk5OSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMDQ2NS5jOTk5IjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAwNDY3LmM5OTkiOlsicmVhZCIsInJlYWQtc3RvcmFnZSJdLCJwaHMwMDIxODcuYzk5OSI6WyJyZWFkIiwicmVhZC1zdG9yYWdlIl0sInBoczAwMjI3Ni5jOTk5IjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwiU0RfUEVUN1E2RjIiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0FROUtWTjVQIjpbInJlYWQtc3RvcmFnZSJdLCJTRF82NTA2NFAyWiI6WyJyZWFkLXN0b3JhZ2UiXSwiU0RfWjZNV0QzSDAiOlsicmVhZC1zdG9yYWdlIl0sIlNEX0ZZQ1I3OFcwIjpbInJlYWQtc3RvcmFnZSJdLCJTRF9ZNlZSRzZNRCI6WyJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMTczLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyMTc0LmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNTkxLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNTkwLmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXSwicGhzMDAyNjI3LmMxIjpbInJlYWQiLCJyZWFkLXN0b3JhZ2UiXX19fSwiYXVkIjpbIlYzVFBQRERMMlQxODB1UFRHeTBiWklEMTE0S1BzZDBqY3ppMERveFYiLCJodHRwczovL2RhdGEua2lkc2ZpcnN0ZHJjLm9yZy91c2VyIl0sImF0X2hhc2giOiJXaHZCQW15QkI3MEM5QjFseFhTVk93In0.pGhEez8ohIkdooXbeDEwvlwlUjL4KrQyZkr0S5JRklJukMgkEqPho6WTptjqeu4CmrMy4ZMwJh4u6DIYFbNdHYP22N0yuo_FhuuEBytYNS-3Gh8TKuIvZf5Y0qB2ENtUhl0ovQOjCgQ1iBeHrCJWwzmH2kcOMt5jqgyaQsvM3FsJMWPOggDellaR5_QgEq1bWf-ZM5P6toX54ine2SRlztHzohNdWOYRcB0zVcvARQQS2ZW-OrNIwkhfawOypYophiEAs4AUwdKq37F_jyuwd4YG3DQPd758wjVPPdML4d9A8iv_Ls8O4AjUd86FOxM4WJcJdp8CUKOx-qgGTuUtNQ";

        String decompressed = decompress(accessToken);

        Assert.assertEquals(accessToken, decompressed);
    }
}
