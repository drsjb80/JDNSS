package edu.msudenver.cs.jdnss;

import java.net.*;
import java.io.*;

public class MClient
{
    public static void main (String[] args)
    {
	int l = args.length / 2;

	String questions[] = new String[l];
	int types[] = new int[l];
	int classes[] = new int[l];

	for (int i = 0; i < l; i++)
	{
	    questions[i] = args[i*2];
	    types[i] = Utils.mapStringToType (args[i*2+1]);
	    classes[i] = 1;
	}

	try
	{
	    int port = 5353;

	    InetAddress group = InetAddress.getByName ("224.0.0.251");
	    MulticastSocket s = new MulticastSocket (port);

	    s.joinGroup (group);

	    Query q = new Query (1000, questions, types, classes);
	    byte send[] = q.getBuffer();

	    s.send (new DatagramPacket (send, send.length, group, port));

	    byte b[] = new byte[1500];
	    DatagramPacket receive = new DatagramPacket (b, b.length);
	    boolean done = false;

	    while (!done)
	    {
		s.receive (receive);

		q = new Query (Utils.trimbytearray (receive.getData(),
		    receive.getLength()));

		if (q.getQR() == false)
		{
		    // System.out.println ("Query");
		    continue;
		}

		if (q.getId() != 1000)
		{
		    // System.out.println ("Id = " + q.getId());
		    continue;
		}

		System.out.println (q);
		done = true;
	    }

	    s.leaveGroup (group);
	}
	catch (Exception e) { e.printStackTrace(); }
    }
}
