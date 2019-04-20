package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TCPThreadTest {
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
}