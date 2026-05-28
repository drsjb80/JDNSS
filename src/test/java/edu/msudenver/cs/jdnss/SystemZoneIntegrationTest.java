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

    @Test
    public void soa1ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("SOA1.com");

        final Query query = new Query(buildQuery(0x611b, "SOA1.com", RRCode.SOA));
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
    public void soa2ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("SOA2.com");

        final Query query = new Query(buildQuery(0x611c, "SOA2.com", RRCode.SOA));
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
    public void nsaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("NSA.com");

        final Query query = new Query(buildQuery(0x611d, "NSA.com", RRCode.NS));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 147, 153, 170, 17));
        Assert.assertTrue(containsIpv4(result, 147, 153, 170, 27));
    }

    @Test
    public void nsaaaaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("NSAAAA.com");

        final Query query = new Query(buildQuery(0x611e, "NSAAAA.com", RRCode.NS));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void mxaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MXA.com");

        final Query query = new Query(buildQuery(0x611f, "MXA.com", RRCode.MX));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(4, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
        Assert.assertTrue(containsIpv4(result, 147, 153, 170, 17));
        Assert.assertTrue(containsIpv4(result, 147, 153, 170, 27));
    }

    @Test
    public void mxaaaaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MXAAAA.com");

        final Query query = new Query(buildQuery(0x6120, "MXAAAA.com", RRCode.MX));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(4, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 0, 2));
    }

    @Test
    public void noMxZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("oneA.com");

        final Query query = new Query(buildQuery(0x6121, "oneA.com", RRCode.MX));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void ttlMinimumZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTLminimum.com");

        final Query query = new Query(buildQuery(0x6122, "www.TTLminimum.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void ttlRrZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTLRR.com");

        final Query query = new Query(buildQuery(0x6123, "www.TTLRR.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void ttlDefaultZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTLdefault.com");

        final Query query = new Query(buildQuery(0x6124, "www.TTLdefault.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(4, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 3));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 4));
    }

    @Test
    public void ttl1ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTL1.com");

        final Query query = new Query(buildQuery(0x6125, "www.TTL1.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
    }

    @Test
    public void ttl2ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTL2.com");

        final Query query = new Query(buildQuery(0x6126, "www.TTL2.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
    }

    @Test
    public void ttl3ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("TTL3.com");

        final Query query = new Query(buildQuery(0x6127, "www.TTL3.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
    }

    @Test
    public void subdomainZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("subdomain.com");

        final Query query = new Query(buildQuery(0x6128, "www.subdomain.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
    }

    @Test
    public void subdomainNestedOriginZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("subdomain.com");

        final Query query = new Query(buildQuery(0x6129, "www.www.subdomain.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 2, 2));
    }

    @Test
    public void originZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("ORIGIN.com");

        final Query query = new Query(buildQuery(0x612a, "www.sub.ORIGIN.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void nonAtSoaZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("nonAT.com");

        final Query query = new Query(buildQuery(0x612b, "www.nonAT.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
    }

    @Test
    public void noHostCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("SOA.com");

        final Query query = new Query(buildQuery(0x612c, "www.SOA.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(3, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void mxAfCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MXA.com");

        final Query query = new Query(buildQuery(0x612d, "one.MXA.com", RRCode.AAAA));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void anoAaaaCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MXA.com");

        final Query query = new Query(buildQuery(0x612e, "one.MXA.com", RRCode.AAAA));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void badDomainCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("MXA.com");

        final Query query = new Query(buildQuery(0x612f, "one.MXAAA.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(5, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void cnameCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("CNAME.com");

        final Query query = new Query(buildQuery(0x6130, "www.CNAME.com", RRCode.CNAME));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void cnameAResolutionParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("CNAME.com");

        final Query query = new Query(buildQuery(0x6131, "www.CNAME.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(3, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void cnameAaaaCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("CNAMEAAAA.com");

        final Query query = new Query(buildQuery(0x6132, "www.CNAMEAAAA.com", RRCode.CNAME));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void cnameToAaaaResolutionParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("CNAMEAAAA.com");

        final Query query = new Query(buildQuery(0x6133, "www.CNAMEAAAA.com", RRCode.AAAA));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(3, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void aaaaNoATypeCaseParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("CNAMEAAAA.com");

        final Query query = new Query(buildQuery(0x6134, "ftp.CNAMEAAAA.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
    }

    @Test
    public void rr1ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR1.com", "www.RR1.com", 0x6135);
    }

    @Test
    public void rr2ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR2.com", "www.RR2.com", 0x6136);
    }

    @Test
    public void rr3ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR3.com", "www.RR3.com", 0x6137);
    }

    @Test
    public void rr4ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR4.com", "RR4.com", 0x6138);
    }

    @Test
    public void rr5ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR5.com", "RR5.com", 0x6139);
    }

    @Test
    public void rr6ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR6.com", "RR6.com", 0x613a);
    }

    @Test
    public void rr7ZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        assertSimpleAQueryCase("RR7.com", "RR7.com", 0x613b);
    }

    @Test
    public void includeZoneFileParsesAndAnswersLikeSystemFixture() throws Exception {
        installZoneFromSystemFixture("INCLUDE.com");

        final Query query = new Query(buildQuery(0x613c, "www.INCLUDE.com", RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    private void assertSimpleAQueryCase(final String zoneName, final String qName, final int id)
            throws Exception {
        installZoneFromSystemFixture(zoneName);

        final Query query = new Query(buildQuery(id, qName, RRCode.A));
        query.parseQueries("");

        final byte[] result = new Response(query, true).getBytes();

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(2, readUInt16(result, 8));
        Assert.assertEquals(2, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 1));
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
