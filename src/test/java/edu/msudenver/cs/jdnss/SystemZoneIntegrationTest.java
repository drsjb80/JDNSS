package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SystemZoneIntegrationTest {
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
    }

    @After
    public void tearDown() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void oneAZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("oneA.com");

        final Query query = new Query(buildQuery(0x6112, "oneA.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void hostZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("host.com");

        final Query query = new Query(buildQuery(0x6113, "www.host.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void atZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("AT.com");

        final Query query = new Query(buildQuery(0x6114, "AT.com", RRCode.NS));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(3, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void soaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("SOA.com");

        final Query query = new Query(buildQuery(0x6115, "SOA.com", RRCode.SOA));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void nsZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("NS.com");

        final Query query = new Query(buildQuery(0x6116, "NS.com", RRCode.NS));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void mxZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MX.com");

        final Query query = new Query(buildQuery(0x6117, "MX.com", RRCode.MX));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void hinfoZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("HINFO.com");

        final Query query = new Query(buildQuery(0x6118, "www.HINFO.com", RRCode.HINFO));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void txtZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TXT.com");

        final Query query = new Query(buildQuery(0x6119, "www.TXT.com", RRCode.TXT));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void aaaaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("AAAA.com");

        final Query query = new Query(buildQuery(0x611a, "www.AAAA.com", RRCode.AAAA));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(6, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    private static void installZoneFromSystemFixture(final String zoneName) throws Exception {
        final BindZone zone = new BindZone(zoneName);
        try (InputStream in = new FileInputStream("src/test/system/zone_files/" + zoneName)) {
            final Parser parser = new Parser(in, zone);
            parser.RRs();
        }

        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(zoneName, zone);
    }

    private static byte[] buildQuery(final int id, final String qName, final RRCode type) {
        byte[] queryBytes = new byte[] {
                (byte) ((id >> 8) & 0xff), (byte) (id & 0xff),
                0x01, 0x00,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        };

        queryBytes = Utils.combine(queryBytes, DnsNameCodec.convertString(qName));
        queryBytes = Utils.combine(queryBytes, new byte[] {
                (byte) ((type.getCode() >> 8) & 0xff), (byte) (type.getCode() & 0xff),
                0x00, 0x01
        });
        return queryBytes;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        final Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }

    private static int readUInt16(final byte[] bytes, final int offset) {
        return (unsignedByte(bytes[offset]) << 8) | unsignedByte(bytes[offset + 1]);
    }

    private static int unsignedByte(final byte value) {
        return value & 0xff;
    }

    private static boolean containsIpv4(final byte[] bytes, final int a, final int b,
                                        final int c, final int d) {
        for (int i = 0; i <= bytes.length - 4; i++) {
            if (unsignedByte(bytes[i]) == a
                    && unsignedByte(bytes[i + 1]) == b
                    && unsignedByte(bytes[i + 2]) == c
                    && unsignedByte(bytes[i + 3]) == d) {
                return true;
            }
        }
        return false;
    }
}
