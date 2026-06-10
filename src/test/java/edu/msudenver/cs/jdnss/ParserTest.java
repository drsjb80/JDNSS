package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    public void includeLoadsRelativePathFromZoneFileDirectory() throws Exception {
        Path zoneDirectory = Files.createTempDirectory("jdnss-zone-dir");
        Path includeFile = zoneDirectory.resolve("relative-include.zone");
        Files.writeString(includeFile, "api A 192.168.1.99\n", StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE relative-include.zone");

            BindZone zone = parseZone(zoneText, "test.com", zoneDirectory);
            List<RR> aRecords = zone.get(RRCode.A, "api.test.com");

            Assert.assertEquals(1, aRecords.size());
            Assert.assertEquals(3600, aRecords.get(0).getTtl());
        } finally {
            Files.deleteIfExists(includeFile);
            Files.deleteIfExists(zoneDirectory);
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

    @Test
    public void originDirectiveChangesSubsequentRelativeNames() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "www A 192.168.1.2",
                "$ORIGIN alt.test.com.",
                "api A 192.168.1.3");

        BindZone zone = parseZone(zoneText, "test.com");

        Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        Assert.assertEquals(1, zone.get(RRCode.A, "api.alt.test.com").size());
    }

    @Test
    public void ttlSuffixHoursAreConvertedToSeconds() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ 1H IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "www 2H IN A 192.168.1.2");

        BindZone zone = parseZone(zoneText, "test.com");
        List<RR> aRecords = zone.get(RRCode.A, "www.test.com");

        Assert.assertEquals(1, aRecords.size());
        Assert.assertEquals(7200, aRecords.get(0).getTtl());
    }

    @Test
    public void ttlSuffixWeeksAreConvertedToSeconds() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ 1W IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "www A 192.168.1.2");

        BindZone zone = parseZone(zoneText, "test.com");
        List<RR> aRecords = zone.get(RRCode.A, "www.test.com");

        Assert.assertEquals(1, aRecords.size());
        Assert.assertEquals(604800, aRecords.get(0).getTtl());
    }

    @Test
    public void includeWithOriginDirectiveAffectsFollowingRecords() throws Exception {
        Path includeFile = Files.createTempFile("jdnss-include-origin", ".zone");
        Files.writeString(includeFile, String.join("\n",
                "$ORIGIN child.test.com.",
                "api A 192.168.1.99"), StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + includeFile.toAbsolutePath(),
                    "www A 192.168.1.2");

            BindZone zone = parseZone(zoneText, "test.com");

            Assert.assertEquals(1, zone.get(RRCode.A, "api.child.test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A, "www.child.test.com").size());
        } finally {
            Files.deleteIfExists(includeFile);
        }
    }

    @Test
    public void nestedIncludeLoadsInnerFileRecords() throws Exception {
        Path innerInclude = Files.createTempFile("jdnss-include-inner", ".zone");
        Path outerInclude = Files.createTempFile("jdnss-include-outer", ".zone");

        Files.writeString(innerInclude, "nested A 192.168.1.77\n", StandardCharsets.UTF_8);
        Files.writeString(outerInclude,
                "$INCLUDE " + innerInclude.toAbsolutePath() + "\n",
                StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + outerInclude.toAbsolutePath());

            BindZone zone = parseZone(zoneText, "test.com");

            Assert.assertEquals(1, zone.get(RRCode.A, "nested.test.com").size());
        } finally {
            Files.deleteIfExists(innerInclude);
            Files.deleteIfExists(outerInclude);
        }
    }

    @Test
    public void malformedLineDoesNotAbortFollowingRecords() throws Exception {
        String zoneText = String.join("\n",
                "$ORIGIN test.com.",
                "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                "broken ?",
                "www A 192.168.1.2");

        BindZone zone = parseZone(zoneText, "test.com");

        Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        Assert.assertEquals(0, zone.get(RRCode.A, "broken.test.com").size());
    }

    @Test
    public void nestedIncludeWithMissingInnerFileDoesNotAbortOuterParsing() throws Exception {
        Path outerInclude = Files.createTempFile("jdnss-include-outer-missing", ".zone");
        Path missingInner = outerInclude.resolveSibling("definitely-missing-jdnss-inner.zone");

        Files.writeString(outerInclude,
                String.join("\n",
                        "$INCLUDE " + missingInner,
                        "nested A 192.168.1.77",
                        "api A 192.168.1.88"),
                StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + outerInclude.toAbsolutePath(),
                    "www A 192.168.1.2");

            BindZone zone = parseZone(zoneText, "test.com");

            Assert.assertEquals(1, zone.get(RRCode.A, "nested.test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A, "api.test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        } finally {
            Files.deleteIfExists(outerInclude);
        }
    }

    @Test
    public void includeCycleIsDetectedWithoutInfiniteRecursion() throws Exception {
        Path recursiveInclude = Files.createTempFile("jdnss-include-cycle", ".zone");

        Files.writeString(recursiveInclude,
                String.join("\n",
                        "$INCLUDE " + recursiveInclude.toAbsolutePath(),
                        "loop A 192.168.1.77"),
                StandardCharsets.UTF_8);

        try {
            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + recursiveInclude.toAbsolutePath(),
                    "www A 192.168.1.2");

            BindZone zone = parseZone(zoneText, "test.com");

            Assert.assertEquals(1, zone.get(RRCode.A, "loop.test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        } finally {
            Files.deleteIfExists(recursiveInclude);
        }
    }

    @Test
    public void includeDepthLimitSkipsTooDeepIncludes() throws Exception {
        Path includeDirectory = Files.createTempDirectory("jdnss-include-depth");
        int maxIndex = Parser.MAX_INCLUDE_DEPTH + 1;

        try {
            Path[] includeFiles = new Path[maxIndex + 1];
            for (int i = 0; i <= maxIndex; i++) {
                includeFiles[i] = includeDirectory.resolve("depth-" + i + ".zone");
            }

            for (int i = maxIndex; i >= 0; i--) {
                StringBuilder content = new StringBuilder();
                if (i < maxIndex) {
                    content.append("$INCLUDE ")
                            .append(includeFiles[i + 1].toAbsolutePath())
                            .append("\n");
                }
                content.append("node").append(i).append(" A 192.168.1.")
                        .append((i % 200) + 1)
                        .append("\n");

                Files.writeString(includeFiles[i], content.toString(), StandardCharsets.UTF_8);
            }

            String zoneText = String.join("\n",
                    "$ORIGIN test.com.",
                    "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                    "$INCLUDE " + includeFiles[0].toAbsolutePath(),
                    "www A 192.168.1.2");

            BindZone zone = parseZone(zoneText, "test.com");

            Assert.assertEquals(1, zone.get(RRCode.A, "node0.test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A,
                    "node" + (Parser.MAX_INCLUDE_DEPTH - 1) + ".test.com").size());
            Assert.assertEquals(0, zone.get(RRCode.A,
                    "node" + Parser.MAX_INCLUDE_DEPTH + ".test.com").size());
            Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        } finally {
            Files.walk(includeDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private static BindZone parseZone(final String zoneText, final String zoneName) throws Exception {
        return parseZone(zoneText, zoneName, null);
    }

    private static BindZone parseZone(final String zoneText, final String zoneName,
                                      final Path includeDirectory) throws Exception {
        BindZone zone = new BindZone(zoneName);
        ByteArrayInputStream input = new ByteArrayInputStream(zoneText.getBytes(StandardCharsets.UTF_8));
        Parser parser = new Parser(input, zone,
                includeDirectory == null ? null : includeDirectory.toFile());
        parser.RRs();
        return zone;
    }
}
