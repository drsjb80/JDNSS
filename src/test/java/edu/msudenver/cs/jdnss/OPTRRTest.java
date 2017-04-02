package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class OPTRRTest {
    @Test
<<<<<<< HEAD
    public void doBitTest()
    {
=======
    public void doBitTest() {
>>>>>>> Added ability to parse additional section
        byte[] bytes = new byte[16];

        String rrName = "Test";

        // Populate RR Name
        byte[] name = new String(rrName).getBytes();

        bytes[0] = (byte) rrName.length();
        for (int i = 1; i <= rrName.length(); i++)
        {
            bytes[i] = name[i - 1];
        }

        // Set Resource Record type to 41 (OPTRR)
        bytes[7] = 41;

        //Set DO Bit to a 1
        bytes[12] = (byte) 128;

        OPTRR rr = new OPTRR(bytes);

        Assert.assertTrue(rr.DOBit);

        //Set DO Bit to a 0
        bytes[12] = (byte) 0;
        rr = new OPTRR(bytes);
        Assert.assertFalse(rr.DOBit);
    }

    @Test
<<<<<<< HEAD
    public void payloadSizeTest()
    {
=======
    public void payloadSizeTest() {
>>>>>>> Added ability to parse additional section
        byte[] bytes = new byte[16];

        String rrName = "Test";

        // Populate RR Name
        byte[] name = new String(rrName).getBytes();

        bytes[0] = (byte) rrName.length();
        for(int i = 1; i <= rrName.length(); i++)
        {
            bytes[i] = name[i - 1];
        }

        // Set Resource Record type to 41 (OPTRR)
        bytes[7] = 41;

        //Byte array payloadsize = 0
        OPTRR rr = new OPTRR(bytes);
        Assert.assertEquals(512, rr.payloadSize);

        bytes[8] = 0;
        bytes[9] = 100;

        //Byte array payloadsize = 100
        rr = new OPTRR(bytes);
        Assert.assertEquals(512, rr.payloadSize);

        //Byte array payloadsize = 512
        bytes[8] = 2;
        bytes[9] = 0;

        rr = new OPTRR(bytes);

        Assert.assertEquals(512, rr.payloadSize);

        //Byte array payloadsize = 550
        bytes[8] = 2;
        bytes[9] = 38;

        rr = new OPTRR(bytes);

        Assert.assertEquals(550, rr.payloadSize);

    }

    @Test
<<<<<<< HEAD
    public void byteSizeTest()
    {
        byte[] bytes = new byte[16];

        String rrName = "Test";
=======
    public void byteSizeTest(){
        byte[] bytes = new byte[16];

        String rrName = "Test";

>>>>>>> Added ability to parse additional section
        // Populate RR Name
        byte[] name = new String(rrName).getBytes();

        bytes[0] = (byte) rrName.length();
<<<<<<< HEAD
        for (int i = 1; i <= rrName.length(); i++)
        {
=======
        for(int i = 1; i <= rrName.length(); i++) {
>>>>>>> Added ability to parse additional section
            bytes[i] = name[i - 1];
        }

        // Set Resource Record type to 41 (OPTRR)
        bytes[7] = 41;
<<<<<<< HEAD
=======

>>>>>>> Added ability to parse additional section
        OPTRR rr = new OPTRR(bytes);
        Assert.assertEquals(15, rr.getByteSize());

        bytes[15] = 10; //Add 10 to byte size
        rr = new OPTRR(bytes);
        Assert.assertEquals(25, rr.getByteSize());

        bytes[14] = 1; //Add 256 to byte size
        rr = new OPTRR(bytes);
        Assert.assertEquals(281, rr.getByteSize());
    }
}
