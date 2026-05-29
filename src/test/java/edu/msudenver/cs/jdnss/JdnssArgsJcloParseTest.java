package edu.msudenver.cs.jdnss;

import edu.msudenver.cs.jclo.JCLO;
import org.junit.Assert;
import org.junit.Test;

public class JdnssArgsJcloParseTest {
    @Test
    public void parseWiresBooleanAndScalarOptions() {
        jdnssArgs args = new jdnssArgs();

        new JCLO(args).parse(new String[] {
                "--threads=7",
                "--once",
                "--help",
                "--version"
        });

        Assert.assertEquals(7, args.getThreads());
        Assert.assertTrue(args.isOnce());
        Assert.assertTrue(args.isHelp());
        Assert.assertTrue(args.isVersion());
    }

    @Test
    public void parseAppendsIpAddressesWithoutPreNormalization() {
        jdnssArgs args = new jdnssArgs();

        new JCLO(args).parse(new String[] {"--IPaddresses=UDP@127.0.0.1@5300"});

        Assert.assertNotNull(args.IPaddresses);
        Assert.assertEquals(4, args.IPaddresses.length);
        Assert.assertEquals("UDP@127.0.0.1@5300", args.IPaddresses[3]);
    }

    @Test
    public void parseCapturesPositionalZonesAsAdditional() {
        jdnssArgs args = new jdnssArgs();

        new JCLO(args).parse(new String[] {"test.com", "example.com"});

        Assert.assertArrayEquals(new String[] {"test.com", "example.com"}, args.getAdditional());
    }

    @Test
    public void parseWiresAdditionalReflectedFields() {
        jdnssArgs args = new jdnssArgs();

        new JCLO(args).parse(new String[] {
                "--backlog=9",
                "--logLevel=DEBUG",
                "--serverSecret=0123456789abcdef",
                "--keystoreFile=server.jks",
                "--keystorePassword=changeit",
                "--debugSSL"
        });

        Assert.assertEquals(9, args.backlog);
        Assert.assertEquals(JDNSSLogLevels.DEBUG, args.logLevel);
        Assert.assertEquals("0123456789abcdef", args.serverSecret);
        Assert.assertEquals("server.jks", args.keystoreFile);
        Assert.assertEquals("changeit", args.keystorePassword);
        Assert.assertTrue(args.debugSSL);
    }
}
