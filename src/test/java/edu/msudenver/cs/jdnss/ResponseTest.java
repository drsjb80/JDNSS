package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by beatys on 10/17/17.
 */
public class ResponseTest {
    private static final String ZONE_NAME = "test.com";

    private Response response;
    private Query query;
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        originalZones = new HashMap<>(liveZones);

        liveZones.clear();
        liveZones.put(ZONE_NAME, createTestZone());

        query = new Query(queryNoCookie);
        query.parseQueries("");
        response = new Response(query, true);
    }

    @After
    public void tearDown() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void getBytesIncludesQuestionAndAnswer() {
        final byte[] result = response.getBytes();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.length > queryNoCookie.length);

        Assert.assertEquals(0x5f, unsignedByte(result[0]));
        Assert.assertEquals(0x3e, unsignedByte(result[1]));
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0x04, unsignedByte(result[2]) & 0x04);
        Assert.assertEquals(0x00, unsignedByte(result[3]) & 0x80);

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    private static BindZone createTestZone() {
        final BindZone zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME,
                new SOARR(ZONE_NAME, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add(ZONE_NAME, new ARR(ZONE_NAME, 3600, "192.168.1.2"));
        return zone;
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

    private final byte[] queryNoCookie = {(byte) 0x5f, (byte) 0x3e, (byte) 0x01
            , (byte) 0x20, (byte) 0x00, (byte) 0x01, (byte) 0x00
            , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
            , (byte) 0x00, (byte) 0x04, (byte) 0x74, (byte) 0x65
            , (byte) 0x73, (byte) 0x74, (byte) 0x03, (byte) 0x63
            , (byte) 0x6f, (byte) 0x6d, (byte) 0x00, (byte) 0x00
            , (byte) 0x01, (byte) 0x00, (byte) 0x01};
}
