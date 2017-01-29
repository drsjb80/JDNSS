package edu.msudenver.cs.javaln;

import java.io.*;
import java.util.logging.*;
import java.util.Date;
import java.text.DateFormat;

/**
 * Add method line numbers to logging.
 */

public class LineNumberFormatter extends SimpleFormatter
{
    private static String us[] =
    {
	"edu.msudenver.cs.javaln.syslog.SyslogFormatter",
	"edu.msudenver.cs.javaln.syslog.SyslogHandler"
    };

    private static boolean findUs (String s)
    {
        for (int i = 0; i < us.length; i++)
	    if (s.equals (us[i]))
	       return (true);

	return (false);
    }

    public static int getLineNumber()
    {
	StackTraceElement ste = null;
	StackTraceElement u[] = new Throwable().getStackTrace();

	/*
	** start at 1, ignoring this method, and march through until
	** we're out of the logging methods.
	*/
	for (int i = 1; i < u.length; i++)
	{
	    String className = u[i].getClassName();

	    // System.out.println (className + " " + u[i].getLineNumber());

	    if (! (className.startsWith ("java.util.logging")) &&
		! (className.startsWith ("edu.msudenver.cs.javaln")))
	    {
		ste = u[i];
		break;
	    }
	}

	return (ste == null ? -1 : ste.getLineNumber());
    }

    public String format (LogRecord rec)
    {

	String linesep = System.getProperty ("line.separator");
	String s = super.format (rec).replaceFirst (linesep,
	    " " + getLineNumber() + linesep);

	return (s);
    }

    public static void main (String args[])
    {
	ConsoleHandler ch = new ConsoleHandler();
	ch.setFormatter (new LineNumberFormatter());
	ch.setLevel (Level.FINEST);

        Logger logger = Logger.getLogger ("global");
	logger.addHandler (ch);
	logger.setUseParentHandlers (false);
	logger.setLevel (Level.FINEST);

	logger.severe ("this is a test");
	logger.finest ("this is another");
    }
}
