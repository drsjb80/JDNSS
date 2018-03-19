package edu.msudenver.cs.jdnss;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

class MC extends UDP
{
    public MC() throws IOException
    {
        final int port = JDNSS.jargs.getMCport();
        final String address = JDNSS.jargs.getMCaddress();

        MulticastSocket msocket = new MulticastSocket(port);

        /*
        try
        {
        */
            // msocket.setNetworkInterface(NetworkInterface.getByName("en0"));
            msocket.setTimeToLive(255);
            msocket.joinGroup(InetAddress.getByName(address));
            // logger.fatal(msocket.getInterface());
            // logger.fatal(msocket.getNetworkInterface());
        /*
        }
        catch (UnknownHostException uhe)
        {
            logger.catching(uhe);
            throw uhe;
        }
        catch (IOException ioe)
        {
            logger.catching(ioe);
            throw ioe;
        }
        */

        // now UDP and MC are the same, so use the run from UDP
        dsocket = msocket;
        System.out.println(dsocket);

        logger.traceExit();
    }
}
