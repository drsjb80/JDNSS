package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class QueryTest
{
    byte[] buffer = {(byte) 0xaa, (byte) 0xd8, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x03, (byte) 0x77, (byte) 0x77, (byte) 0x77,
            (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x01};
    byte[] header = {(byte) 0xaa, (byte) 0xd8, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            };
    byte[] rawQuery = {(byte) 0x03, (byte) 0x77, (byte) 0x77, (byte) 0x77,
            (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x01};
    byte[] bindBuffer = {(byte) 0x6b, (byte) 0xcd, (byte) 0x01, (byte) 0x20,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x03, (byte) 0x77, (byte) 0x77, (byte) 0x77,
            (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x29, (byte) 0x10, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x0c, (byte) 0x00, (byte) 0x0a, (byte) 0x00,
            (byte) 0x08, (byte) 0xc2, (byte) 0x0f, (byte) 0xef,
            (byte) 0xfa, (byte) 0xb4, (byte) 0xa5, (byte) 0xdf,
            (byte) 0x5e};
    Query query = new Query(buffer);
    Query bindQuery = new Query(bindBuffer);

    @Before
    public void setUp() throws Exception {
        query.parseQueries();
        bindQuery.parseQueries();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void Query() throws Exception {
        Assert.assertEquals(1, query.getHeader().getNumQuestions());
        Assert.assertEquals(1, bindQuery.getHeader().getNumQuestions());
        Assert.assertEquals(1, bindQuery.getHeader().getNumAdditionals());
    }

    @Test
    public void parseQueries() throws Exception {
    }

    @Test
    public void getBuffer() throws Exception {
        Assert.assertTrue(Arrays.equals(buffer, query.getBuffer()));
    }

    @Test
    public void getQueries() throws Exception {
        Queries[] queries = query.getQueries();
        Assert.assertEquals(1, queries.length);
        Assert.assertTrue(queries[0].getName().equals("www.test.com"));
        Assert.assertEquals(Utils.A, queries[0].getType());
        Assert.assertEquals(1, queries[0].getQclass());

        Queries[] bindQueries = bindQuery.getQueries();
        Assert.assertEquals(1, bindQueries.length);
        Assert.assertEquals(1, bindQuery.getHeader().getNumAdditionals());
        Assert.assertTrue(bindQueries[0].getName().equals("www.test.com"));
        Assert.assertEquals(Utils.A, bindQueries[0].getType());
        Assert.assertEquals(1, bindQueries[0].getQclass());
        // Assert.assertEquals(Utils.OPT, bindQueries[1].getType());
        // Assert.assertEquals(1, bindQueries[1].getQclass());
    }

    @Test
    public void getRawQueries() throws Exception {
        Assert.assertTrue(Arrays.equals(rawQuery, query.getRawQueries()));
    }

    @Test
    public void getZone() throws Exception {
    }
    /*
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
    */

    /*
    @Test
    public void parseSingleAdditionalTest()
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};
        Query query = new Query(1000, questions, types, classes);

        byte[] buffer = new byte[16];
        String rrName = "Test";
        // Populate RR Name
        byte[] name = new String(rrName).getBytes();
        buffer[0] = (byte) rrName.length();

        for(int i = 1; i <= rrName.length(); i++) {
            buffer[i] = name[i - 1];
        }
        // Set Resource Record type to 41 (OPTRR)
        buffer[7] = 41;
        query.parseAdditional(buffer, 1);
        Assert.assertNotNull(query.optrr);
    }
    */


    /*
    @Test
    public void parseMultipleAdditionalTest()
    {
        String questions[] = new String[]{"www.pipes.org"};
        int types[] = new int[]{Utils.A};
        int classes[] = new int[]{1};

        byte[] buffer = new byte[32];

        String firstName = "Test";
        String secondName = "Tast";

        byte[] nameByte = new String(firstName).getBytes();

        buffer[0] = (byte) firstName.length();

        for(int i = 1; i <= firstName.length(); i++) {
            buffer[i] = nameByte[i - 1];
        }
        buffer[7] = 41;
        buffer[23] = 42;


        byte[] secondByte = new String(secondName).getBytes();
        buffer[16] = (byte) secondName.length();

        for(int i = 17; i <= secondName.length() + 16; i++) {
            buffer[i] = secondByte[i - 17];
        }

        Query query = new Query(1000, questions, types, classes);
        query.parseAdditional(buffer, 2);
        Assert.assertNotNull(query.optrr);

        query = new Query(1000, questions, types, classes);
        //Remove OPTRR type
        buffer[7] = 0;
        buffer[23] = 0;
        query.parseAdditional(buffer, 2);
        Assert.assertNull(query.optrr);
    }
    */
}
