package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class UtilsTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void reverse_IP()
    {
        Assert.assertEquals (Utils.reverseIP ("192.168.1.2"), "2.1.168.192");
        Assert.assertEquals (Utils.reverseIP (""), "");
        Assert.assertEquals (Utils.reverseIP (null), null);
        Assert.assertEquals (Utils.reverseIP ("foo"), "foo");
        Assert.assertEquals (Utils.reverseIP ("."), ".");
        Assert.assertEquals (Utils.reverseIP ("..."), "...");
    }

    @Test
    public void getByte()
    {
        Assert.assertEquals (Utils.getByte (0xfff00f00, 1), (byte) 0);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 2), (byte) 15);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 3), (byte) 240);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 4), (byte) 255);
    }

    @Test
    public void getTwoBytes()
    {
        byte good1[] = {(byte) 0x0f, (byte) 0x00};
        byte test1[] = Utils.getTwoBytes (0xfff00f00, 2);
        Assert.assertTrue (good1[0] == test1[0]);
        Assert.assertTrue (good1[1] == test1[1]);

        byte good2[] = {(byte) 0xf0, (byte) 0x0f};
        byte test2[] = Utils.getTwoBytes (0xfff00f00, 3);
        Assert.assertTrue (good2[0] == test2[0]);
        Assert.assertTrue (good2[1] == test2[1]);

        byte good3[] = {(byte) 0xff, (byte) 0xf0};
        byte test3[] = Utils.getTwoBytes (0xfff00f00, 4);
        Assert.assertTrue (good3[0] == test3[0]);
        Assert.assertTrue (good3[1] == test3[1]);

        exception.expect (IllegalArgumentException.class);
        Utils.getTwoBytes (0xfff00f00, 1);
        Utils.getTwoBytes (0xfff00f00, 5);
    }
}
