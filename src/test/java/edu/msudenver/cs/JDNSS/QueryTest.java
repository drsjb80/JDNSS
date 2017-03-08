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
            "Questions: 1\tAnswers: 0\n" +
            "Authority RR's: 0\tAdditional RR's: 0\n" +
            "QR: false\tAA: true\n" +
            "TC: false\tRD: false\n" +
            "RA: false\tAD: false\tCD: false\topcode: 0\n" +
            "rcode: 0\n" +
            "Name: www.pipes.org Type: 1 Class: 0";
        Assert.assertEquals (query.toString(), expectedQuery);
    }
}
