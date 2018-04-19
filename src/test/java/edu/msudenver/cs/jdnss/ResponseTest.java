package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by beatys on 10/17/17.
 */
public class ResponseTest {

    Query query;
    Response response;

    @Before
    public void setUp() throws Exception {
        String[] args = {"--serverSecret=01234567890123456789abcdef", "test.com"};
        JDNSS.main(args);
        this.query = new Query(queryNoCookie);
        this.query.parseQueries("/0:0:0:0:0:0:0:1");
        this.response = new Response(query, true);
    }


    @Test
    public void getBytesTest() {
        Assert.assertArrayEquals(expectedResponseNoCookie, response.getBytes());
    }

    @After
    public void tearDown() throws Exception {
    }

    byte[] queryNoCookie = {(byte) 0x5f, (byte) 0x3e, (byte) 0x01
            , (byte) 0x20, (byte) 0x00, (byte) 0x01, (byte) 0x00
            , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
            , (byte) 0x00, (byte) 0x04, (byte) 0x74, (byte) 0x65
            , (byte) 0x73, (byte) 0x74, (byte) 0x03, (byte) 0x63
            , (byte) 0x6f, (byte) 0x6d, (byte) 0x00, (byte) 0x00
            , (byte) 0x01, (byte) 0x00, (byte) 0x01};

    byte[] expectedResponseNoCookie = {(byte) 0x5f, (byte) 0x3e, (byte) 0x85, (byte) 0x00
            , (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01
            , (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00
            , (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73
            , (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f
            , (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01
            , (byte) 0x00, (byte) 0x01, (byte) 0x04, (byte) 0x74
            , (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x03
            , (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x00
            , (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01
            , (byte) 0x00, (byte) 0x01, (byte) 0x51, (byte) 0x80
            , (byte) 0x00, (byte) 0x04, (byte) 0xc0, (byte) 0xa8
            , (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x74
            , (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x03
            , (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x00
            , (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x01
            , (byte) 0x00, (byte) 0x01, (byte) 0x51, (byte) 0x80
            , (byte) 0x00, (byte) 0x0e, (byte) 0x03, (byte) 0x6f
            , (byte) 0x6e, (byte) 0x65, (byte) 0x04, (byte) 0x74
            , (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x03
            , (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x00
            , (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73
            , (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f
            , (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x02
            , (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01
            , (byte) 0x51, (byte) 0x80, (byte) 0x00, (byte) 0x0e
            , (byte) 0x03, (byte) 0x74, (byte) 0x77, (byte) 0x6f
            , (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73
            , (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f
            , (byte) 0x6d, (byte) 0x00};
}
