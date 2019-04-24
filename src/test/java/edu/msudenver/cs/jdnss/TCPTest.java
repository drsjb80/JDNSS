package edu.msudenver.cs.jdnss;

import org.junit.Test;

import java.net.InetAddress;
import java.net.ServerSocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TCPTest {
    @Test
    public void TCP() throws Exception {

        InetAddress address = InetAddress.getByAddress(new byte[]{10, 10, 10, 10});
//        int backlog = 1;
//        int port = 53;

        ServerSocket serverSocket = mock(ServerSocket.class);

        JDNSS.jargs = mock(jdnssArgs.class);
        InetAddress inetAddress = mock(InetAddress.class);

        when(InetAddress.getByName("a.domain.name")).thenReturn(address);
        when(JDNSS.jargs.getIPaddress()).thenReturn("10.10.10.10");

        TCP tcp = new TCP();
    }
}
