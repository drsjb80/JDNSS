package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MulticastSocket;

public class MCTest {

    private static class TestMC extends MC {
        boolean configured;

        TestMC(final String[] parts) {
            super(parts);
        }

        @Override
        MulticastSocket createMulticastSocket(final int port) throws IOException {
            return new MulticastSocket();
        }

        @Override
        void configureMulticastSocket(final MulticastSocket msocket, final String address,
                                      final int port) {
            configured = true;
        }
    }

    private static class FailingMC extends MC {
        FailingMC(final String[] parts) {
            super(parts);
        }

        @Override
        MulticastSocket createMulticastSocket(final int port) throws IOException {
            throw new IOException("socket-create-failed");
        }
    }

    @Test
    public void constructorAssignsSocketWhenConfigurationSucceeds() {
        TestMC mc = new TestMC(new String[] {"MC", "224.0.0.251", "5353"});

        Assert.assertTrue(mc.configured);
        Assert.assertNotNull(mc.dsocket);
        mc.dsocket.close();
    }

    @Test
    public void constructorLeavesSocketNullWhenCreationFails() {
        FailingMC mc = new FailingMC(new String[] {"MC", "224.0.0.251", "5353"});

        Assert.assertNull(mc.dsocket);
    }
}
