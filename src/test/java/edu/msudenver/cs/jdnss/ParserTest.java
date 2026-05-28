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

    private static BindZone parseZone(final String zoneText, final String zoneName) throws Exception {
        BindZone zone = new BindZone(zoneName);
        ByteArrayInputStream input = new ByteArrayInputStream(zoneText.getBytes(StandardCharsets.UTF_8));
        Parser parser = new Parser(input, zone);
        parser.RRs();
        return zone;
    }
}
