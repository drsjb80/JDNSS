package edu.msudenver.cs.jdnss;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BindZoneTest
{

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void bindZone()
    {
        BindZone z = new BindZone ("name");

        z.add ("name",
                new SOARR ("domain", "server", "contact", 1, 2, 3, 4, 5, 6));
        z.add ("www", new ARR ("www", 0, "1.2.3.4"));
        z.add ("www", new ARR ("www", 0, "4.3.1.1"));
        z.add ("", new MXRR ("4.3.2.1", 0, "4.3.2.1", 10));

        /*
        String expectedZ = "---- Zone name -----\n" +
                "SOA: name: [SOA:  domain = domain, server = server, contact = contact, serial = 1, refresh = 2, " +
                "retry = 3, expire = 4, minimum = 5, name = domain, type = SOA, TTL = 6] \n" +
                "A: www: [ADDR: address = 1.2.3.4, name = www, type = A, TTL = 0, ADDR: address = 4.3.1.1, name = www, "+
                "type = A, TTL = 0] \n" +
                "AAAA: \n" +
                "CNAME: \n" +
                "MX: : [MX: host = 4.3.2.1, preference = 10, name = 4.3.2.1, type = MX, TTL = 0] \n" +
                "NS: \n" +
                "PTR: \n" +
                "TXT: \n" +
                "HINFO: \n" +
                "DNSKEY: \n" +
                "DNSRRSIG: \n" +
                "NSEC: \n" +
                "NSEC3: \n" +
                "NSEC3PARAM: \n" +
                "--------";
        Assert.assertEquals (z.toString(), expectedZ);
        */

        String expectedwww = "[ADDR: address = 1.2.3.4, name = www, type = A, TTL = 0, " +
                "ADDR: address = 4.3.1.1, name = www, type = A, TTL = 0]";
        // FIXME Assert.assertEquals (z.get (RRCode.A, "www").toString(), expectedwww);

        // exception.expect (AssertionError.class);
        // Assert.assertNull (z.get (RRCode.A, "WWW"));
    }

}