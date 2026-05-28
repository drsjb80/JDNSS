package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
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
    public void getLength() throws Exception {
        Socket socket = mock(Socket.class);
        byte b[] = {0, 0};
        ByteArrayInputStream inputstream = new ByteArrayInputStream(b);
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        TCPThread tt = new TCPThread(socket);
        P38.call("openStreams", tt);

        Object o = P38.call("getLength", tt);
        Assert.assertEquals(0, o);
    }

    @Test
    public void getLengthNonZero() throws Exception {
        Socket socket = mock(Socket.class);
        byte[] b = {0x00, 0x1b};
        ByteArrayInputStream inputstream = new ByteArrayInputStream(b);
        ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

        when(socket.getInputStream()).thenReturn(inputstream);
        when(socket.getOutputStream()).thenReturn(outputstream);

        TCPThread tt = new TCPThread(socket);
        P38.call("openStreams", tt);

        Object o = P38.call("getLength", tt);
        Assert.assertEquals(27, o);
    }

    @Test
    public void closeStreamsClosesSocketAndStreams() throws Exception {
        Socket socket = mock(Socket.class);
        InputStream inputStream = mock(InputStream.class);
        OutputStream outputStream = mock(OutputStream.class);

        when(socket.getInputStream()).thenReturn(inputStream);
        when(socket.getOutputStream()).thenReturn(outputStream);

        TCPThread tt = new TCPThread(socket);
        P38.call("openStreams", tt);
        P38.call("closeStreams", tt);

        verify(inputStream).close();
        verify(outputStream).close();
        verify(socket).close();
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
    }

    @Test
    public void runHandlesIOException() throws Exception {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenThrow(new IOException("boom"));

        TCPThread tt = new TCPThread(socket);
        tt.run();
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