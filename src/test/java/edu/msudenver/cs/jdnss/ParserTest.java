package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ParserTest {

    @Test
    public void parsesOriginTtlAndCoreRecords() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "$TTL 300",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "@ IN NS ns1.test.com.",
                "www A 192.168.1.2",
                "mail MX 10 mail.test.com.",
                "txt TXT \"hello\"");

        BindZone zone = parseZone(zoneText, "test.com");

        List<RR> aRecords = zone.get(RRCode.A, "www.test.com");
        List<RR> nsRecords = zone.get(RRCode.NS, "test.com");
        List<RR> mxRecords = zone.get(RRCode.MX, "mail.test.com");
        List<RR> txtRecords = zone.get(RRCode.TXT, "txt.test.com");

        Assert.assertEquals(1, aRecords.size());
        Assert.assertEquals(1, nsRecords.size());
        Assert.assertEquals(1, mxRecords.size());
        Assert.assertEquals(1, txtRecords.size());

        Assert.assertEquals(300, aRecords.get(0).getTtl());
        Assert.assertEquals(300, mxRecords.get(0).getTtl());
    }

    @Test
    public void soaTtlActsAsFloorForExplicitRecordTtl() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ 3600 IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "www 100 IN A 192.168.1.2");

        BindZone zone = parseZone(zoneText, "test.com");
        List<RR> aRecords = zone.get(RRCode.A, "www.test.com");

        Assert.assertEquals(1, aRecords.size());
        Assert.assertEquals(3600, aRecords.get(0).getTtl());
    }

    @Test
    public void includeLoadsRecordsFromExternalFile() throws Exception {
        Path includeFile = Files.createTempFile("jdnss-include", ".zone");
        Files.writeString(includeFile, "api A 192.168.1.99\n", StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + includeFile.toAbsolutePath());

            BindZone zone = parseZone(zoneText, "test.com");
            List<RR> aRecords = zone.get(RRCode.A, "api.test.com");

            Assert.assertEquals(1, aRecords.size());
            Assert.assertEquals(3600, aRecords.get(0).getTtl());
        } finally {
            Files.deleteIfExists(includeFile);
        }
    }

    @Test
    public void includeMissingFileDoesNotAbortParsing() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "$INCLUDE /tmp/definitely-does-not-exist-jdnss.zone",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "www A 192.168.1.2");

        BindZone zone = parseZone(zoneText, "test.com");
        List<RR> aRecords = zone.get(RRCode.A, "www.test.com");

        Assert.assertEquals(1, aRecords.size());
    }

    @Test
    public void parsesArpaPointerShorthandName() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN 1.168.192.in-addr.arpa.",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "2 IN PTR host.test.com.");

        BindZone zone = parseZone(zoneText, "1.168.192.in-addr.arpa");
        List<RR> ptrRecords = zone.get(RRCode.PTR, "2.1.168.192.in-addr.arpa");

        Assert.assertEquals(1, ptrRecords.size());
        Assert.assertEquals("host.test.com", ptrRecords.get(0).getString());
    }

    @Test
    public void parsesHinfoAndAaaaRecords() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "host AAAA 2001:db8::1",
                "host HINFO \"x86_64\" \"linux\"");

        BindZone zone = parseZone(zoneText, "test.com");
        List<RR> aaaaRecords = zone.get(RRCode.AAAA, "host.test.com");
        List<RR> hinfoRecords = zone.get(RRCode.HINFO, "host.test.com");

        Assert.assertEquals(1, aaaaRecords.size());
        Assert.assertEquals(1, hinfoRecords.size());
        Assert.assertTrue(aaaaRecords.get(0) instanceof AAAARR);
        Assert.assertTrue(hinfoRecords.get(0) instanceof HINFORR);
    }

    private static BindZone parseZone(final String zoneText, final String zoneName) throws Exception {
        BindZone zone = new BindZone(zoneName);
        ByteArrayInputStream input = new ByteArrayInputStream(zoneText.getBytes(StandardCharsets.UTF_8));
        Parser parser = new Parser(input, zone);
        parser.RRs();
        return zone;
    }
}
