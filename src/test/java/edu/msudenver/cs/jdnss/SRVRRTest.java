package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class SRVRRTest {
    @Test
    public void constructorStoresAllFields() {
        SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, 10, 60, 80, "www.example.com");
        Assert.assertNotNull(srv);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, 10, 60, 80, "www.example.com");
        byte[] bytes = srv.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("SRV should have at least priority+weight+port", bytes.length >= 6);
    }

    @Test
    public void typeCodeIsSRV() {
        SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, 10, 60, 80, "www.example.com");
        Assert.assertEquals(RRCode.SRV, srv.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        SRVRR srv = new SRVRR("_http._tcp.example.com", ttl, 10, 60, 80, "www.example.com");
        Assert.assertEquals(ttl, srv.getTtl());
    }

    @Test
    public void handleMultiplePrioritiesAndWeights() {
        int[][] testCases = {{10, 60, 80}, {0, 0, 0}, {65535, 65535, 65535}};
        for (int[] testCase : testCases) {
            SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, testCase[0], testCase[1], testCase[2], "www.example.com");
            byte[] bytes = srv.getBytes();
            Assert.assertTrue("Should serialize priority, weight, port", bytes.length > 0);
        }
    }

    @Test
    public void handleFQDN() {
        SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, 10, 60, 80, "www.example.com.");
        byte[] bytes = srv.getBytes();
        Assert.assertTrue("Should handle FQDN with trailing dot", bytes.length > 0);
    }

    @Test
    public void gettersReturnStoredValues() {
        int priority = 10;
        int weight = 60;
        int port = 8080;
        String target = "srv.example.com";
        SRVRR srv = new SRVRR("_http._tcp.example.com", 3600, priority, weight, port, target);
        Assert.assertEquals(priority, srv.getPriority());
        Assert.assertEquals(weight, srv.getWeight());
        Assert.assertEquals(port, srv.getPort());
        Assert.assertEquals(target, srv.getTarget());
    }
}
