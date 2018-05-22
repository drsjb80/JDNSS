package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class RRsTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void rrsTest1() {
        byte one[] =
        {
            0x03, 'w', 'w', 'w',
            0x04, 't', 'e', 's', 't',
            0x03, 'c', 'o', 'm',
            0x00,
            0x00, 0x01, 0x00, 0x00
        };

        RRs rrs1 = new RRs(one, 1, 0, 0, 0);

        String expectedRRs1 = "Questions:\n" +
            "name = www.test.com, type = A, TTL = 0";

        Assert.assertEquals(rrs1.toString(), expectedRRs1);
    }

    @Test
    public void rrsTest2() {
        byte two[] =
        {
            0x03, 'w', 'w', 'w',
            0x04, 't', 'e', 's', 't',
            0x03, 'c', 'o', 'm',
            0x00,
            0x00, 0x01, 0x00, 0x00,
            0x04, 'm', 'a', 'i', 'l',
            (byte) 0xc0, 0x04, 0x00,
            0x00, 0x01, 0x00, 0x00
        };

        RRs rrs2 = new RRs(two, 2, 0, 0, 0);

        String expectedRRs2 = "Questions:\n" +
            "name = www.test.com, type = A, TTL = 0\n" +
            "name = mail.test.com, type = A, TTL = 0";

        Assert.assertEquals(rrs2.toString(), expectedRRs2);
    }

    // The two following check for bad names.
    @Test
    public void rrsTest3() {
        byte three[] = {(byte) 0xc0, 0x00, 0x01, 0x00, 0x00, 0x00};

        exception.expect (AssertionError.class);
        RRs rrs3 = new RRs(three, 1, 0, 0, 0);

        String expectedRRs3 = "Questions:\n" +
            "null";

        Assert.assertEquals(rrs3.toString(), expectedRRs3);
    }

    @Test
    public void rrsTest4() {
        byte four[] =
            {
                0x03, 'w', 'w', 'w',
                0x04, 't', 'e', 's', 't',
                0x03, 'c', 'o', 'm',
                (byte) 0xc0, 0x00, 0x0,
                0x00, 0x01, 0x00, 0x000
            };

        exception.expect(AssertionError.class);
        RRs rrs4 = new RRs(four, 1, 0, 0, 0);

        String expectedRRs4 = "Questions:\n" +
                "null";

        Assert.assertEquals(rrs4.toString(), expectedRRs4);
    }
    @Test
    public void nsecGetBytesTest() {
        byte expected[] = {0x04, 'h', 'o', 's', 't',
                0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
                0x03, 'c', 'o', 'm', 0x00,0x00,
                0x00, 0x06, 0x40, 0x01, 0x00, 0x00, 0x00, 0x03};
        Set<RRCode> rrSet = new HashSet<>();
        rrSet.add(RRCode.A);
        rrSet.add(RRCode.MX);
        rrSet.add(RRCode.RRSIG);
        rrSet.add(RRCode.NSEC);
        NSECRR nsec = new NSECRR("alfa.example.com.",
                86400,"host.example.com.", rrSet);

        Assert.assertArrayEquals(expected, nsec.getBytes());
    }

    @Test
    public void dnskeyGetBytesTest() {
        DNSKEYRR dnsKey = new DNSKEYRR("test.com", 86400, 256, 3, 10, "AwEAAbrtgQCC5bN+BM3ZC2OB+R045DPPwSbu" +
                "5vU5xrZKy+6AHHuDzOn6TbnIE8vSwGK+vJQ1" +
                "TAM6RK8OGnrChaZt8U80C2CNCkFFXeKi5rXM" +
                "CUkwI0qLppOVqDXBCxzu5Rzed5l6WecKNb2y" +
                "3BmwfZRzYaQH8ggkH9sdfYnyAbzM+FEQNCTq" +
                "cI3MrE3JPHB4WJnaKWiRSS6T+zNKnBsjaB/d" +
                "HcfKns9HXt25ZcIAYQBGTO5ZM5G9ZVvAnow4" +
                "fVifAyKFXwRCcKdFxfgTLHGzNnMggbyHOJ6t" +
                "RMYUFAuLayg0hMCBBIEapwwcv2fTIxwvd2ct" +
                "2Nsov+Q+YpZTfESxAxWMzgM=");

        byte[] expected = {
                  (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x0a
                , (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x01
                , (byte)0xba, (byte)0xed, (byte)0x81, (byte)0x00
                , (byte)0x82, (byte)0xe5, (byte)0xb3, (byte)0x7e
                , (byte)0x04, (byte)0xcd, (byte)0xd9, (byte)0x0b
                , (byte)0x63, (byte)0x81, (byte)0xf9, (byte)0x1d
                , (byte)0x38, (byte)0xe4, (byte)0x33, (byte)0xcf
                , (byte)0xc1, (byte)0x26, (byte)0xee, (byte)0xe6
                , (byte)0xf5, (byte)0x39, (byte)0xc6, (byte)0xb6
                , (byte)0x4a, (byte)0xcb, (byte)0xee, (byte)0x80
                , (byte)0x1c, (byte)0x7b, (byte)0x83, (byte)0xcc
                , (byte)0xe9, (byte)0xfa, (byte)0x4d, (byte)0xb9
                , (byte)0xc8, (byte)0x13, (byte)0xcb, (byte)0xd2
                , (byte)0xc0, (byte)0x62, (byte)0xbe, (byte)0xbc
                , (byte)0x94, (byte)0x35, (byte)0x4c, (byte)0x03
                , (byte)0x3a, (byte)0x44, (byte)0xaf, (byte)0x0e
                , (byte)0x1a, (byte)0x7a, (byte)0xc2, (byte)0x85
                , (byte)0xa6, (byte)0x6d, (byte)0xf1, (byte)0x4f
                , (byte)0x34, (byte)0x0b, (byte)0x60, (byte)0x8d
                , (byte)0x0a, (byte)0x41, (byte)0x45, (byte)0x5d
                , (byte)0xe2, (byte)0xa2, (byte)0xe6, (byte)0xb5
                , (byte)0xcc, (byte)0x09, (byte)0x49, (byte)0x30
                , (byte)0x23, (byte)0x4a, (byte)0x8b, (byte)0xa6
                , (byte)0x93, (byte)0x95, (byte)0xa8, (byte)0x35
                , (byte)0xc1, (byte)0x0b, (byte)0x1c, (byte)0xee
                , (byte)0xe5, (byte)0x1c, (byte)0xde, (byte)0x77
                , (byte)0x99, (byte)0x7a, (byte)0x59, (byte)0xe7
                , (byte)0x0a, (byte)0x35, (byte)0xbd, (byte)0xb2
                , (byte)0xdc, (byte)0x19, (byte)0xb0, (byte)0x7d
                , (byte)0x94, (byte)0x73, (byte)0x61, (byte)0xa4
                , (byte)0x07, (byte)0xf2, (byte)0x08, (byte)0x24
                , (byte)0x1f, (byte)0xdb, (byte)0x1d, (byte)0x7d
                , (byte)0x89, (byte)0xf2, (byte)0x01, (byte)0xbc
                , (byte)0xcc, (byte)0xf8, (byte)0x51, (byte)0x10
                , (byte)0x34, (byte)0x24, (byte)0xea, (byte)0x70
                , (byte)0x8d, (byte)0xcc, (byte)0xac, (byte)0x4d
                , (byte)0xc9, (byte)0x3c, (byte)0x70, (byte)0x78
                , (byte)0x58, (byte)0x99, (byte)0xda, (byte)0x29
                , (byte)0x68, (byte)0x91, (byte)0x49, (byte)0x2e
                , (byte)0x93, (byte)0xfb, (byte)0x33, (byte)0x4a
                , (byte)0x9c, (byte)0x1b, (byte)0x23, (byte)0x68
                , (byte)0x1f, (byte)0xdd, (byte)0x1d, (byte)0xc7
                , (byte)0xca, (byte)0x9e, (byte)0xcf, (byte)0x47
                , (byte)0x5e, (byte)0xdd, (byte)0xb9, (byte)0x65
                , (byte)0xc2, (byte)0x00, (byte)0x61, (byte)0x00
                , (byte)0x46, (byte)0x4c, (byte)0xee, (byte)0x59
                , (byte)0x33, (byte)0x91, (byte)0xbd, (byte)0x65
                , (byte)0x5b, (byte)0xc0, (byte)0x9e, (byte)0x8c
                , (byte)0x38, (byte)0x7d, (byte)0x58, (byte)0x9f
                , (byte)0x03, (byte)0x22, (byte)0x85, (byte)0x5f
                , (byte)0x04, (byte)0x42, (byte)0x70, (byte)0xa7
                , (byte)0x45, (byte)0xc5, (byte)0xf8, (byte)0x13
                , (byte)0x2c, (byte)0x71, (byte)0xb3, (byte)0x36
                , (byte)0x73, (byte)0x20, (byte)0x81, (byte)0xbc
                , (byte)0x87, (byte)0x38, (byte)0x9e, (byte)0xad
                , (byte)0x44, (byte)0xc6, (byte)0x14, (byte)0x14
                , (byte)0x0b, (byte)0x8b, (byte)0x6b, (byte)0x28
                , (byte)0x34, (byte)0x84, (byte)0xc0, (byte)0x81
                , (byte)0x04, (byte)0x81, (byte)0x1a, (byte)0xa7
                , (byte)0x0c, (byte)0x1c, (byte)0xbf, (byte)0x67
                , (byte)0xd3, (byte)0x23, (byte)0x1c, (byte)0x2f
                , (byte)0x77, (byte)0x67, (byte)0x2d, (byte)0xd8
                , (byte)0xdb, (byte)0x28, (byte)0xbf, (byte)0xe4
                , (byte)0x3e, (byte)0x62, (byte)0x96, (byte)0x53
                , (byte)0x7c, (byte)0x44, (byte)0xb1, (byte)0x03
                , (byte)0x15, (byte)0x8c, (byte)0xce, (byte)0x03};

        Assert.assertArrayEquals(expected, dnsKey.getBytes());
    }

    @Test
    public void nsecGetBytesTest2() {
        byte expected[] = {0x04, 'h', 'o', 's', 't',
                0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
                0x03, 'c', 'o', 'm', 0x00,0x00,
                0x00, 0x07, 0x40, 0x00, 0x00, 0x08, 0x00, 0x03, 0x30};
        Set<RRCode> rrSet = new HashSet<>();
        rrSet.add(RRCode.A);
        rrSet.add(RRCode.AAAA);
        rrSet.add(RRCode.RRSIG);
        rrSet.add(RRCode.NSEC);
        rrSet.add(RRCode.NSEC3);
        rrSet.add(RRCode.NSEC3PARAM);
        NSECRR nsec = new NSECRR("alfa.example.com.",
                86400,"host.example.com.", rrSet);
        Assert.assertArrayEquals(expected, nsec.getBytes());
    }

    @Test
    public void rrSigGetBytesTest () {
        int firstValue = 0;
        int secondValue = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss:z");
            Date fdt = sdf.parse(20180515033855L + ":UTC");
            long epoch = fdt.getTime();
            firstValue = (int) (epoch / 1000);
            fdt = sdf.parse(20180415033855L + ":UTC");
            epoch = fdt.getTime();
            secondValue = (int) (epoch / 1000);
        } catch(ParseException e){
            Assertion.fail();
        }
        RRSIG rr = new RRSIG("test.com", 86400, RRCode.SOA,
        10, 2, 86400, firstValue, secondValue, 12023,
        "test.com.", "P4y2MsOiIHQMZKRJFrfqt3w8DO7TVsZ0Tg3B" +
                "K4weuxNzNJ+ccJEC+BPqeJRDNN7vPf3hpoQ7" +
                "d4M45sEE/ikwH0zedckURqNftFCvkmmUUVYU" +
                "H6Ym/bmmuE4rcXP44kfv5hVo3CyO1KEySMfF" +
                "hcZAl7nMJUA1+XwhHyvz5xb4lfu1mXWpKDOg" +
                "WhPhSsols6vxEYVn07WaV/tcrNV0EWFT0FiC" +
                "z52LDHCek397ldMR8jAySXsp8Ojo54WvaXh8" +
                "L8HrxGAQ9aWv8iE5DMFeBM0hSZUqFuFfpPaV" +
                "Lu9Nu/cbSYbe5sPSacegi5K9OMPpScXqcyhY" +
                "T8DCCZIm3WqNQPN95w==");

        byte[] expected = {(byte)0x00, (byte)0x06, (byte)0x0a
                , (byte)0x02, (byte)0x00, (byte)0x01, (byte)0x51
                , (byte)0x80, (byte)0x5a, (byte)0xfa, (byte)0x56
                , (byte)0x4f, (byte)0x5a, (byte)0xd2, (byte)0xc9
                , (byte)0x4f, (byte)0x2e, (byte)0xf7, (byte)0x04
                , (byte)0x74, (byte)0x65, (byte)0x73, (byte)0x74
                , (byte)0x03, (byte)0x63, (byte)0x6f, (byte)0x6d
                , (byte)0x00, (byte) 0x00
                , (byte)0x3f, (byte)0x8c, (byte)0xb6, (byte)0x32
                , (byte)0xc3, (byte)0xa2, (byte)0x20, (byte)0x74
                , (byte)0x0c, (byte)0x64, (byte)0xa4, (byte)0x49
                , (byte)0x16, (byte)0xb7, (byte)0xea, (byte)0xb7
                , (byte)0x7c, (byte)0x3c, (byte)0x0c, (byte)0xee
                , (byte)0xd3, (byte)0x56, (byte)0xc6, (byte)0x74
                , (byte)0x4e, (byte)0x0d, (byte)0xc1, (byte)0x2b
                , (byte)0x8c, (byte)0x1e, (byte)0xbb, (byte)0x13
                , (byte)0x73, (byte)0x34, (byte)0x9f, (byte)0x9c
                , (byte)0x70, (byte)0x91, (byte)0x02, (byte)0xf8
                , (byte)0x13, (byte)0xea, (byte)0x78, (byte)0x94
                , (byte)0x43, (byte)0x34, (byte)0xde, (byte)0xef
                , (byte)0x3d, (byte)0xfd, (byte)0xe1, (byte)0xa6
                , (byte)0x84, (byte)0x3b, (byte)0x77, (byte)0x83
                , (byte)0x38, (byte)0xe6, (byte)0xc1, (byte)0x04
                , (byte)0xfe, (byte)0x29, (byte)0x30, (byte)0x1f
                , (byte)0x4c, (byte)0xde, (byte)0x75, (byte)0xc9
                , (byte)0x14, (byte)0x46, (byte)0xa3, (byte)0x5f
                , (byte)0xb4, (byte)0x50, (byte)0xaf, (byte)0x92
                , (byte)0x69, (byte)0x94, (byte)0x51, (byte)0x56
                , (byte)0x14, (byte)0x1f, (byte)0xa6, (byte)0x26
                , (byte)0xfd, (byte)0xb9, (byte)0xa6, (byte)0xb8
                , (byte)0x4e, (byte)0x2b, (byte)0x71, (byte)0x73
                , (byte)0xf8, (byte)0xe2, (byte)0x47, (byte)0xef
                , (byte)0xe6, (byte)0x15, (byte)0x68, (byte)0xdc
                , (byte)0x2c, (byte)0x8e, (byte)0xd4, (byte)0xa1
                , (byte)0x32, (byte)0x48, (byte)0xc7, (byte)0xc5
                , (byte)0x85, (byte)0xc6, (byte)0x40, (byte)0x97
                , (byte)0xb9, (byte)0xcc, (byte)0x25, (byte)0x40
                , (byte)0x35, (byte)0xf9, (byte)0x7c, (byte)0x21
                , (byte)0x1f, (byte)0x2b, (byte)0xf3, (byte)0xe7
                , (byte)0x16, (byte)0xf8, (byte)0x95, (byte)0xfb
                , (byte)0xb5, (byte)0x99, (byte)0x75, (byte)0xa9
                , (byte)0x28, (byte)0x33, (byte)0xa0, (byte)0x5a
                , (byte)0x13, (byte)0xe1, (byte)0x4a, (byte)0xca
                , (byte)0x25, (byte)0xb3, (byte)0xab, (byte)0xf1
                , (byte)0x11, (byte)0x85, (byte)0x67, (byte)0xd3
                , (byte)0xb5, (byte)0x9a, (byte)0x57, (byte)0xfb
                , (byte)0x5c, (byte)0xac, (byte)0xd5, (byte)0x74
                , (byte)0x11, (byte)0x61, (byte)0x53, (byte)0xd0
                , (byte)0x58, (byte)0x82, (byte)0xcf, (byte)0x9d
                , (byte)0x8b, (byte)0x0c, (byte)0x70, (byte)0x9e
                , (byte)0x93, (byte)0x7f, (byte)0x7b, (byte)0x95
                , (byte)0xd3, (byte)0x11, (byte)0xf2, (byte)0x30
                , (byte)0x32, (byte)0x49, (byte)0x7b, (byte)0x29
                , (byte)0xf0, (byte)0xe8, (byte)0xe8, (byte)0xe7
                , (byte)0x85, (byte)0xaf, (byte)0x69, (byte)0x78
                , (byte)0x7c, (byte)0x2f, (byte)0xc1, (byte)0xeb
                , (byte)0xc4, (byte)0x60, (byte)0x10, (byte)0xf5
                , (byte)0xa5, (byte)0xaf, (byte)0xf2, (byte)0x21
                , (byte)0x39, (byte)0x0c, (byte)0xc1, (byte)0x5e
                , (byte)0x04, (byte)0xcd, (byte)0x21, (byte)0x49
                , (byte)0x95, (byte)0x2a, (byte)0x16, (byte)0xe1
                , (byte)0x5f, (byte)0xa4, (byte)0xf6, (byte)0x95
                , (byte)0x2e, (byte)0xef, (byte)0x4d, (byte)0xbb
                , (byte)0xf7, (byte)0x1b, (byte)0x49, (byte)0x86
                , (byte)0xde, (byte)0xe6, (byte)0xc3, (byte)0xd2
                , (byte)0x69, (byte)0xc7, (byte)0xa0, (byte)0x8b
                , (byte)0x92, (byte)0xbd, (byte)0x38, (byte)0xc3
                , (byte)0xe9, (byte)0x49, (byte)0xc5, (byte)0xea
                , (byte)0x73, (byte)0x28, (byte)0x58, (byte)0x4f
                , (byte)0xc0, (byte)0xc2, (byte)0x09, (byte)0x92
                , (byte)0x26, (byte)0xdd, (byte)0x6a, (byte)0x8d
                , (byte)0x40, (byte)0xf3, (byte)0x7d, (byte)0xe7
        };

        Assert.assertArrayEquals(expected, rr.getBytes());
    }
}
