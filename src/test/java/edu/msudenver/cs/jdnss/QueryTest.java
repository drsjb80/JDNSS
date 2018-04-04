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
    byte[] digBuffer = {(byte) 0x6b, (byte) 0xcd, (byte) 0x01, (byte) 0x20,
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
    byte[] digBufferDNSSEC = {(byte) 0xdd, (byte) 0xfc, (byte) 0x01,
            (byte) 0x20, (byte) 0x00, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x03, (byte) 0x77, (byte) 0x77,
            (byte) 0x77, (byte) 0x04, (byte) 0x74, (byte) 0x65,
            (byte) 0x73, (byte) 0x74, (byte) 0x03, (byte) 0x63,
            (byte) 0x6f, (byte) 0x6d, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x29, (byte) 0x10, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00,
            (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x0a,
            (byte) 0x00, (byte) 0x08, (byte) 0x13, (byte) 0xf2,
            (byte) 0xcb, (byte) 0x9f, (byte) 0x2a, (byte) 0x20,
            (byte) 0xf7, (byte) 0x68};

    Query query = new Query(buffer);
    Query digQuery = new Query(digBuffer);
    Query digQueryDNSSEC = new Query(digBufferDNSSEC);

    @Before
    public void setUp() throws Exception {
        query.parseQueries("");
        digQuery.parseQueries("");
        digQueryDNSSEC.parseQueries("");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void Query() throws Exception {
        Assert.assertEquals(1, query.getHeader().getNumQuestions());
        Assert.assertEquals(1, digQuery.getHeader().getNumQuestions());
        Assert.assertEquals(1, digQuery.getHeader().getNumAdditionals());
        Assert.assertEquals(1, digQueryDNSSEC.getHeader().getNumQuestions());
        Assert.assertEquals(1, digQueryDNSSEC.getHeader().getNumAdditionals());
    }

    @Test
    public void parseQueries() throws Exception {
        Assert.assertFalse(digQuery.getOptrr().isDNSSEC());
        Assert.assertTrue(digQueryDNSSEC.getOptrr().isDNSSEC());
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
        Assert.assertEquals(RRCode.A, queries[0].getType());
        Assert.assertEquals(1, queries[0].getQclass());

        Queries[] bindQueries = digQuery.getQueries();
        Assert.assertEquals(1, bindQueries.length);
        Assert.assertEquals(1, digQuery.getHeader().getNumAdditionals());
        Assert.assertTrue(bindQueries[0].getName().equals("www.test.com"));
        Assert.assertEquals(RRCode.A, bindQueries[0].getType());
        Assert.assertEquals(1, bindQueries[0].getQclass());
        // Assert.assertEquals(Utils.OPT, bindQueries[1].getType());
        // Assert.assertEquals(1, bindQueries[1].getQclass());
    }

    @Test
    public void testBuildResponseQueries() {
        byte[] questions = query.buildResponseQueries();
        Assert.assertNotNull(questions);
        Assert.assertTrue(questions.length > 0);
        Assert.assertTrue(questions.length > 13);
        Assert.assertEquals(0x03, questions[0]);
        Assert.assertEquals(0x77, questions[1]);
        Assert.assertEquals(0x65, questions[6]);
        Assert.assertEquals(0x6f, questions[11]);
        Assert.assertEquals(0x00, questions[13]);
        Assert.assertEquals(0x01, questions[17]);
    }

    @Test
    public void getZone() throws Exception {
    }
}
