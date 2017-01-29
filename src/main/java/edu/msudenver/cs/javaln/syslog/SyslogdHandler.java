package edu.msudenver.cs.javaln.syslog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.ErrorManager;

// http://www.faqs.org/rfcs/rfc3164.html

/**
 * Send a message to syslogd listening to the UDP port (typically 514) on a
 * host (typically localhost)
 *
 * @author	Steve Beaty
 */

public class SyslogdHandler extends SyslogHandler
{
    private int port = 514;
    private String host = "localhost";

    public int getPort() { return (port); }
    public String getHost() { return (host); }

    /**
     * A constructor to specify both host and port
     *
     * @param	host	the host to send messages to, if null set to "localhost"
     * @param	port	the port to send messages to, if 0 set to 514
     */
    public SyslogdHandler (String host, int port)
    {
	super();

	this.host = host;
	this.port = port;

	setFormatter (new edu.msudenver.cs.javaln.syslog.SyslogdFormatter());
    }

    public SyslogdHandler (String host)
    {
        this (host, 514);
    }

    /**
     * A default constructor for host localhost and port 514
     */
    public SyslogdHandler ()
    {
        this ("localhost", 514);

	String cname = getClass().getName();
	LogManager manager = LogManager.getLogManager();

	String getHost = manager.getProperty (cname + ".host");
	if (getHost != null)
	    host = getHost;

	String getPort = manager.getProperty (cname + ".port");
	if (getPort != null)
	    port = Integer.parseInt (getPort);
    }

    public synchronized void sendMessage (String message)
    {
	byte buf[] = message.getBytes();

	try
	{
	    DatagramPacket packet = new DatagramPacket (buf, buf.length,
		InetAddress.getByName (host), port);

	    DatagramSocket socket = new DatagramSocket();
	    socket.send (packet);
	    socket.close();
	}
	catch (UnknownHostException uhe)
	{
	    reportError (null, uhe, ErrorManager.WRITE_FAILURE);
	    return;
	}
	catch (SocketException se)
	{
	    reportError (null, se, ErrorManager.WRITE_FAILURE);
	    return;
	}
	catch (IOException ioe)
	{
	    reportError (null, ioe, ErrorManager.WRITE_FAILURE);
	    return;
	}
    }

    /**
     * Unit tests
     */
    public static void main(String[] args)
	throws UnknownHostException, SocketException, InterruptedException
    {
	SyslogdHandler sh = new SyslogdHandler();
        sh.setLevel (Level.FINEST);
        sh.setFormatter (new SyslogdFormatter());

        Logger logger = Logger.getLogger ("global");
        logger.addHandler (sh);
        logger.setUseParentHandlers (false);
        logger.setLevel (Level.FINEST);

        logger.severe ("this is a test");
        logger.finest ("this is another");
    }
}
