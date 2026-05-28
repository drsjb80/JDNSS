package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class MyDatagramSocketTest {

    @Test
    public void toStringContainsExpectedSocketFields() throws Exception {
        MyDatagramSocket socket = new MyDatagramSocket();
        try {
            String value = socket.toString();

            Assert.assertTrue(value.contains("getBroadcast() ="));
            Assert.assertTrue(value.contains("getLocalAddress ="));
            Assert.assertTrue(value.contains("getLocalPort() ="));
            Assert.assertTrue(value.contains("isBound() ="));
            Assert.assertTrue(value.contains("isConnected() ="));
        } finally {
            socket.close();
        }
    }

    @Test
    public void portConstructorCreatesBoundSocket() throws Exception {
        MyDatagramSocket socket = new MyDatagramSocket(0);
        try {
            Assert.assertTrue(socket.isBound());
            Assert.assertTrue(socket.getLocalPort() > 0);
        } finally {
            socket.close();
        }
    }

    @Test
    public void toStringAfterCloseReturnsWithoutThrowing() throws Exception {
        MyDatagramSocket socket = new MyDatagramSocket();
        socket.close();

        String value = socket.toString();

        Assert.assertNotNull(value);
    }
}
