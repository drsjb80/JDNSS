package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MCTest {

    private static class TestMC extends MC {
        boolean configured;
        private final List<Enumeration<NetworkInterface>> enumerations;
        private int enumerationIndex;

        TestMC(final String[] parts) {
            this(parts, Collections.emptyList());
        }

        TestMC(final String[] parts, final List<Enumeration<NetworkInterface>> enumerations) {
            super(parts);
            this.enumerations = enumerations;
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

        @Override
        Enumeration<NetworkInterface> getNetworkInterfaces() {
            if (enumerationIndex < enumerations.size()) {
                return enumerations.get(enumerationIndex++);
            }
            return Collections.emptyEnumeration();
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

    @Test
    public void getNetworkInterfacePrefersUpNonLoopbackMulticast() throws Exception {
        NetworkInterface preferred = Mockito.mock(NetworkInterface.class);
        Mockito.when(preferred.isUp()).thenReturn(true);
        Mockito.when(preferred.isLoopback()).thenReturn(false);
        Mockito.when(preferred.supportsMulticast()).thenReturn(true);

        TestMC mc = new TestMC(new String[] {"MC", "224.0.0.251", "5353"},
                Arrays.asList(Collections.enumeration(Collections.singletonList(preferred))));

        NetworkInterface selected = mc.getNetworkInterface();
        Assert.assertSame(preferred, selected);
    }

    @Test
    public void getNetworkInterfaceFallsBackToAnyMulticast() throws Exception {
        NetworkInterface firstPass = Mockito.mock(NetworkInterface.class);
        Mockito.when(firstPass.isUp()).thenReturn(true);
        Mockito.when(firstPass.isLoopback()).thenReturn(true);
        Mockito.when(firstPass.supportsMulticast()).thenReturn(true);

        NetworkInterface secondPass = Mockito.mock(NetworkInterface.class);
        Mockito.when(secondPass.supportsMulticast()).thenReturn(true);

        TestMC mc = new TestMC(new String[] {"MC", "224.0.0.251", "5353"}, Arrays.asList(
                Collections.enumeration(Collections.singletonList(firstPass)),
                Collections.enumeration(Collections.singletonList(secondPass))
        ));

        NetworkInterface selected = mc.getNetworkInterface();
        Assert.assertSame(secondPass, selected);
    }

    @Test
    public void getNetworkInterfaceReturnsNullWhenNoMulticastInterfaces() throws Exception {
        NetworkInterface firstPass = Mockito.mock(NetworkInterface.class);
        Mockito.when(firstPass.isUp()).thenReturn(false);
        Mockito.when(firstPass.isLoopback()).thenReturn(true);
        Mockito.when(firstPass.supportsMulticast()).thenReturn(false);

        NetworkInterface secondPass = Mockito.mock(NetworkInterface.class);
        Mockito.when(secondPass.supportsMulticast()).thenReturn(false);

        TestMC mc = new TestMC(new String[] {"MC", "224.0.0.251", "5353"}, Arrays.asList(
                Collections.enumeration(Collections.singletonList(firstPass)),
                Collections.enumeration(Collections.singletonList(secondPass))
        ));

        Assert.assertNull(mc.getNetworkInterface());
    }
}
