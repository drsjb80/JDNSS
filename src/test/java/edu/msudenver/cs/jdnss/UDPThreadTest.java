package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.junit.After;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UDPThreadTest {
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("other.com", new BindZone("other.com"));
    }

    @After
    public void tearDown() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void runSendsReplyToOriginalAddressAndPort() throws Exception {
        DatagramSocket socket = mock(DatagramSocket.class);
        InetAddress address = InetAddress.getLoopbackAddress();

        UDPThread thread = new UDPThread(buildQueryPacket(), socket, 5300, address);
        thread.run();

        ArgumentCaptor<DatagramPacket> captor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socket).send(captor.capture());

        DatagramPacket sent = captor.getValue();
        Assert.assertEquals(5300, sent.getPort());
        Assert.assertEquals(address, sent.getAddress());
        Assert.assertTrue(sent.getLength() > 0);
    }

    @Test
    public void runSwallowsSendIOException() throws Exception {
        DatagramSocket socket = mock(DatagramSocket.class);
        InetAddress address = InetAddress.getLoopbackAddress();

        doThrow(new IOException("send failed")).when(socket).send(org.mockito.ArgumentMatchers.any(DatagramPacket.class));

        UDPThread thread = new UDPThread(buildQueryPacket(), socket, 5300, address);
        thread.run();

        verify(socket).send(org.mockito.ArgumentMatchers.any(DatagramPacket.class));
    }

    private static byte[] buildQueryPacket() {
        return new byte[] {
                (byte) 0x5f, (byte) 0x3e, (byte) 0x01, (byte) 0x20,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
                (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
                (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x01
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }
}
