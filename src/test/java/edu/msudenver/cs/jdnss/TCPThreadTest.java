package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TCPThreadTest {
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
    }

    @org.junit.After
    public void tearDown() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void runWritesLengthPrefixedResponse() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("other.com", new BindZone("other.com"));

        Socket socket = mock(Socket.class);
        byte[] packet = buildTcpQueryPacket();
        ByteArrayInputStream inputstream = new ByteArrayInputStream(packet);
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);
        when(socket.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());

        TCPThread tt = new TCPThread(socket);
        tt.run();

        byte[] written = outputstream.toByteArray();
        Assert.assertTrue(written.length > 2);

        int declaredLength = Utils.addThem(written[0], written[1]);
        Assert.assertEquals(written.length - 2, declaredLength);
        verify(socket).close();
    }

    @Test
    public void runHandlesIOException() throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenThrow(new IOException("boom"));

        TCPThread tt = new TCPThread(socket);
        tt.run();

        verify(socket, never()).close();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }

    private static byte[] buildTcpQueryPacket() {
        byte[] query = {(byte) 0x5f, (byte) 0x3e, (byte) 0x01
                , (byte) 0x20, (byte) 0x00, (byte) 0x01, (byte) 0x00
                , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                , (byte) 0x00, (byte) 0x04, (byte) 0x74, (byte) 0x65
                , (byte) 0x73, (byte) 0x74, (byte) 0x03, (byte) 0x63
                , (byte) 0x6f, (byte) 0x6d, (byte) 0x00, (byte) 0x00
                , (byte) 0x01, (byte) 0x00, (byte) 0x01};

        byte[] packet = new byte[query.length + 2];
        packet[0] = Utils.getByte(query.length, 2);
        packet[1] = Utils.getByte(query.length, 1);
        System.arraycopy(query, 0, packet, 2, query.length);
        return packet;
    }
}