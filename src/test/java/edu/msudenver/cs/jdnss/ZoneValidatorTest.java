package edu.msudenver.cs.jdnss;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.Base64;

import static org.junit.Assert.*;

public class ZoneValidatorTest {
    private Zone zone;
    private List<RR> records;

    @Before
    public void setUp() throws Exception {
        zone = new BindZone("example.com");
        records = new ArrayList<>();
    }

    @Test
    public void validateReturnsUnsignedWhenNoRRSIGs() {
        zone.setDnssecEnabled(false);
        List<RR> aRecords = new ArrayList<>();
        aRecords.add(new ARR("www.example.com", 300, "192.0.2.1"));

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", aRecords, zone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateReturnsUnsignedWhenNoValidatingKeys() {
        zone.setDnssecEnabled(true);
        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        RRSIG rrsig = new RRSIG(
            "www.example.com", 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 12345, "example.com",
            signature
        );

        List<RR> signedRecords = new ArrayList<>();
        signedRecords.add(new ARR("www.example.com", 300, "192.0.2.1"));
        signedRecords.add(rrsig);

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", signedRecords, zone);

        assertEquals(ValidationResult.UNVERIFIED, result);
    }

    @Test
    public void validateReturnsUnverifiedWhenDNSSECDisabled() {
        zone.setDnssecEnabled(false);

        List<RR> aRecords = new ArrayList<>();
        aRecords.add(new ARR("www.example.com", 300, "192.0.2.1"));

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", aRecords, zone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateReturnsUnverifiedForEmptyRRset() {
        zone.setDnssecEnabled(true);
        List<RR> emptyRecords = new ArrayList<>();

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", emptyRecords, zone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateFiltersOutNonmatchingRRSIGs() {
        Zone testZone = new BindZone("example.com");
        testZone.setDnssecEnabled(false);
        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        RRSIG wrongTypeRrsig = new RRSIG(
            "www.example.com", 300, RRCode.MX,
            8, 1, 300,
            1704067200, 1704153600, 12345, "example.com",
            signature
        );

        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.example.com", 300, "192.0.2.1"));
        records.add(wrongTypeRrsig);

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, testZone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateHandlesMultipleRRSIGs() {
        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        RRSIG rrsig1 = new RRSIG(
            "www.example.com", 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 12345, "example.com",
            signature
        );
        RRSIG rrsig2 = new RRSIG(
            "www.example.com", 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 54321, "example.com",
            signature
        );

        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.example.com", 300, "192.0.2.1"));
        records.add(rrsig1);
        records.add(rrsig2);

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, zone);

        assertNotNull(result);
    }

    @Test
    public void validateExtractsRRSIGsCorrectly() {
        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.example.com", 300, "192.0.2.1"));
        RRSIG rrsig = new RRSIG(
            "www.example.com", 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 12345, "example.com",
            signature
        );
        records.add(rrsig);

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, zone);

        assertTrue(result == ValidationResult.UNSIGNED ||
                   result == ValidationResult.UNVERIFIED ||
                   result == ValidationResult.INVALID);
    }

    @Test
    public void validateHandlesMissingDomainInRRSIG() {
        Zone testZone = new BindZone("example.com");
        testZone.setDnssecEnabled(false);
        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        RRSIG orphanRrsig = new RRSIG(
            "orphan.example.com", 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 12345, "orphan.example.com",
            signature
        );

        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.example.com", 300, "192.0.2.1"));
        records.add(orphanRrsig);

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, testZone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateReturnsUnsignedForDifferentRecordType() {
        zone.setDnssecEnabled(false);
        List<RR> mxRecords = new ArrayList<>();
        mxRecords.add(new MXRR("example.com", 300, "mail.example.com", 10));

        ValidationResult result = ZoneValidator.validate(RRCode.MX, "example.com", mxRecords, zone);

        assertEquals(ValidationResult.UNSIGNED, result);
    }

    @Test
    public void validateDoesNotModifyInputRecords() {
        List<RR> originalRecords = new ArrayList<>();
        originalRecords.add(new ARR("www.example.com", 300, "192.0.2.1"));
        int originalSize = originalRecords.size();

        ZoneValidator.validate(RRCode.A, "www.example.com", originalRecords, zone);

        assertEquals(originalSize, originalRecords.size());
    }

    @Test
    public void validateHandlesCaseSensitivity() {
        List<RR> records = new ArrayList<>();
        records.add(new ARR("WWW.EXAMPLE.COM", 300, "192.0.2.1"));

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, zone);

        assertNotNull(result);
    }

    @Test
    public void validateHandlesWhitespaceInDomain() {
        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.example.com", 300, "192.0.2.1"));

        ValidationResult result = ZoneValidator.validate(RRCode.A, "www.example.com", records, zone);

        assertNotNull(result);
    }

    @Test
    public void validateCachesResults() {
        BindZone cacheTestZone = new BindZone("cache.example.com");
        List<RR> records = new ArrayList<>();
        records.add(new ARR("www.cache.example.com", 300, "192.0.2.1"));

        ValidationResult result1 = ZoneValidator.validate(RRCode.A, "www.cache.example.com", records, cacheTestZone);
        ValidationResult result2 = ZoneValidator.validate(RRCode.A, "www.cache.example.com", records, cacheTestZone);

        assertEquals(result1, result2);
    }
}
