package edu.msudenver.cs.javaln.syslog;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.ErrorManager;

public class CLIHandler extends SyslogHandler
{
    public CLIHandler ()
    {
	setFormatter (new edu.msudenver.cs.javaln.syslog.CLIFormatter());
    }

    protected synchronized void sendMessage (String message)
    {
	String exec = "logger " + message;
	// System.out.println ("CLIStream exec " + exec);

	try
	{
	    Runtime.getRuntime().exec (exec);
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
	throws InterruptedException
    {
	CLIHandler sh = new CLIHandler();
        sh.setLevel (Level.FINEST);
	sh.setFormatter (new CLIFormatter());

        Logger logger = Logger.getLogger ("global");
        logger.addHandler (sh);
        logger.setUseParentHandlers (false);
        logger.setLevel (Level.FINEST);

        logger.severe ("this is a test");
        logger.finest ("this is another");
    }
}
