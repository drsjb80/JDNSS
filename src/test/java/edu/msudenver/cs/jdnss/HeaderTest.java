package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class HeaderTest {

    /*
      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      ID                       |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    QDCOUNT                    |   Queries
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    ANCOUNT                    |   Answers
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    NSCOUNT                    |   Authorities
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    ARCOUNT                    |   Additionals
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
     */

    @Test
    public void goodConstructor() {
        byte[] one = {0, 1, // ID
                0, 0,       // Flags
                0, 1,       // Queries
                0, 0,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };

        Header header = new Header(one);
        Assert.assertEquals(1, header.getId());

        Assert.assertFalse(header.isQR());
        Assert.assertEquals(0, header.getOpcode());
        Assert.assertFalse(header.isAA());
        Assert.assertFalse(header.isTC());
        Assert.assertFalse(header.isRD());
        Assert.assertFalse(header.isRA());
        Assert.assertEquals(0, header.getRcode());

        Assert.assertEquals(1, header.getNumQuestions());
        Assert.assertEquals(0, header.getNumAnswers());
        Assert.assertEquals(0, header.getNumAuthorities());
        Assert.assertEquals(0, header.getNumAdditionals());
    }

    @Test(expected = AssertionError.class)
    public void noQueries() {
        byte[] one = {0, 1, // ID
                0, 0,       // Flags
                0, 0,       // Queries
                0, 0,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };
        new Header(one);
    }

    @Test(expected = AssertionError.class)
    public void sendAnswers() {
        byte[] one = {0, 1, // ID
                0, 0,       // Flags
                0, 1,       // Queries
                0, 1,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };
        new Header(one);
    }

    @Test(expected = AssertionError.class)
    public void badAA() {
        byte[] one = {0, 1, // ID
                4, 0,      // Flags
                0, 1,       // Queries
                0, 0,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };
        new Header(one);
    }

    @Test(expected = AssertionError.class)
    public void badTC() {
        byte[] one = {0, 1, // ID
                2, 0,      // Flags
                0, 1,       // Queries
                0, 0,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };
        new Header(one);
    }

    @Test(expected = AssertionError.class)
    public void badRA() {
        byte[] one = {0, 1, // ID
                0, (byte) 128,      // Flags
                0, 1,       // Queries
                0, 0,       // Answers
                0, 0,       // Authorities
                0, 0        // Additionals
        };
        new Header(one);
    }

    @Test
    public void getHeader() {
    }
}