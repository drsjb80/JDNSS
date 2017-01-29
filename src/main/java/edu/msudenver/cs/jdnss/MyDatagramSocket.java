package edu.msudenver.cs.jdnss;

/* MyDatagramSocket.java */
import java.net.*;
import java.io.*;

/**
 * A very simple class that extends DatagramSocket in order to create a
 * resonable toString()
 *
 * @author Steve Beaty
 * @version $Id: MyDatagramSocket.java,v 1.1.1.1 2004/06/30 18:42:22 drb80 Exp $
 */

public class MyDatagramSocket extends DatagramSocket
{
    public MyDatagramSocket() throws java.net.SocketException { super(); }

    public MyDatagramSocket(int i) throws java.net.SocketException { super(i); }

    /**
     * The reason for this class's exsistence.
     */
    public String toString()
    {
        String s = "";

        try
        {
            s += "getBroadcast() = " + getBroadcast() + "\n";
            s += "getLocalAddress = " + getLocalAddress().getHostAddress() +
	        "\n";
            s += "getLocalPort() = " + getLocalPort() + "\n";
            s += "getReceiveBufferSize() = " + getReceiveBufferSize() + "\n";
            s += "getReuseAddress() = " + getReuseAddress() + "\n";
            s += "getSendBufferSize() = " + getSendBufferSize() + "\n";
            s += "getSoTimeout() = " + getSoTimeout() + "\n";
            s += "getTrafficClass() = " + getTrafficClass() + "\n";
            s += "isBound() = " + isBound() + "\n";
            s += "isClosed() = " + isClosed() + "\n";
            s += "isConnected() = " + isConnected();
        }
        catch (SocketException e) { e.printStackTrace(); }

        return (s);
    }
}
