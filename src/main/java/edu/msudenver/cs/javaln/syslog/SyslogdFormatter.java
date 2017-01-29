package edu.msudenver.cs.javaln.syslog;

import java.util.logging.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class SyslogdFormatter extends SyslogFormatter
{
    public SyslogdFormatter () { super(); }
    public SyslogdFormatter (int facility) { super (facility); }

    /**
     * Proper formatting a la http://www.faqs.org/rfcs/rfc3164.html
     */
    private static String getDate ()
    {
	Date d = new Date();
	String foo = new SimpleDateFormat ("MMM dd HH:mm:ss").format (d);

	if (foo.charAt (4) == '0')
	{
	    foo = new SimpleDateFormat ("MMM  d HH:mm:ss").format (d);
	}

	return (foo);
    }

    /**
     * A simple formatting routine that returns a properly fomatted prefix
     * for a syslogd message.
     *
     * @param	level	the numerical level of the message
     * @return		a string formatted for the "logger" command
     */
    public String getPrefix (int level)
    {
	int i = facility * 8 + level;
	String ret = "<" + i + ">" + getDate();
	return (ret);
    }

    /**
     * Unit tests
     */
    public static void main (String args[])
    {
	ConsoleHandler ch = new ConsoleHandler();
	ch.setFormatter (new SyslogdFormatter());
	ch.setLevel (Level.FINEST);

        Logger logger = Logger.getLogger ("global");
	logger.addHandler (ch);
	logger.setUseParentHandlers (false);
	logger.setLevel (Level.FINEST);

	logger.severe ("this is a test");
	logger.finest ("this is another");
    }
}
