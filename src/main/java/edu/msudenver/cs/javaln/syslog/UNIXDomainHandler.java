package edu.msudenver.cs.javaln.syslog;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.ErrorManager;

public class UNIXDomainHandler extends SyslogHandler
{
    private static String os;
    private static String arch;

    public UNIXDomainHandler() { super(); }

    /**
     * Find and load the library.
     */
    static
    {
	ErrorManager em = new ErrorManager();

        os = System.getProperty ("os.name").replaceAll (" ", "");
	arch = System.getProperty ("os.arch");
	String name = os + "-" + arch +  "DomainSocket";

	try
	{
	    System.loadLibrary (name);
	}
	catch (UnsatisfiedLinkError ule)
	{
	    // geez, ErrorManager.error DOSN'T handle errors, only
	    // exceptions.  lame.

	    Exception e = new Exception (ule.getCause());
	    String msg = "Didn't find: " + name + " in: " +
		System.getProperties().getProperty ("java.library.path");
	    em.error (msg, e, ErrorManager.OPEN_FAILURE);
	    open = false;
	}
	catch (SecurityException se)
	{
	    String msg = "Security Exception for: " + name;
	    em.error (msg, se, ErrorManager.OPEN_FAILURE);
	    open = false;
	}
    }

    /**
     * The underlying JNI C function
     */
    private native void sendToUNIXSocket (String socketName, String message);

    protected synchronized void sendMessage (String message)
    {
	String socket = os.equals ("MacOSX") ? "/var/run/syslog" : "/dev/log";
	sendToUNIXSocket (socket, message);
    }

    public static void main(String[] args)
	throws InterruptedException
    {
	UNIXDomainHandler sh = new UNIXDomainHandler();
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
