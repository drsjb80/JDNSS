package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class QueryTest
{
    @Test
    public void query()
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};
        Query query = new Query(1000, questions, types, classes);

        String expectedQuery = "Id: 0x3e8\n" +
            "Questions: 1\t" +
            "Answers: 0\n" +
            "Authority RR's: 0\t" +
            "Additional RR's: 0\n" +
            "QR: false\t" +
            "AA: true\t" +
            "TC: false\n" +
            "RD: false\t" +
            "RA: false\t" +
            "AD: false\n" +
            "CD: false\t" +
            "QU: false\n" +
            "opcode: 0\n" +
            "rcode: 0\n" +
            "Name: www.pipes.org Type: 1 Class: 1";
        Assert.assertEquals (query.toString(), expectedQuery);
    }

    @Test
    public void parseSingleAdditionalTest()
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};
        Query query = new Query(1000, questions, types, classes);

        byte[] bytes = new byte[16];
        String rrName = "Test";
        // Populate RR Name
        byte[] name = new String(rrName).getBytes();
        bytes[0] = (byte) rrName.length();

        for(int i = 1; i <= rrName.length(); i++) {
            bytes[i] = name[i - 1];
        }
        // Set Resource Record type to 41 (OPTRR)
        bytes[7] = 41;
        query.parseAdditional(bytes, 1);
        Assert.assertNotNull(query.optrr);
    }


    @Test
    public void parseMultipleAdditionalTest()
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};

        byte[] bytes = new byte[32];

        String firstName = "Test";
        String secondName = "Tast";

        byte[] nameByte = new String(firstName).getBytes();

        bytes[0] = (byte) firstName.length();

        for(int i = 1; i <= firstName.length(); i++) {
            bytes[i] = nameByte[i - 1];
        }
        bytes[7] = 41;
        bytes[23] = 42;


        byte[] secondByte = new String(secondName).getBytes();
        bytes[16] = (byte) secondName.length();

        for(int i = 17; i <= secondName.length() + 16; i++) {
            bytes[i] = secondByte[i - 17];
        }

        Query query = new Query(1000, questions, types, classes);
        query.parseAdditional(bytes, 2);
        Assert.assertNotNull(query.optrr);

        query = new Query(1000, questions, types, classes);
        //Remove OPTRR type
        bytes[7] = 0;
        bytes[23] = 0;
        query.parseAdditional(bytes, 2);
        Assert.assertNull(query.optrr);
    }
}
