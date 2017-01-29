
package edu.msudenver.cs.javaln.syslog;

// http://www.faqs.org/rfcs/rfc3164.html

import java.util.logging.*;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Format a message so that it is acceptable to the UNIX command line
 * interface "logger" command.
 *
 * @author	Steve Beaty
 */

public class CLIFormatter extends SyslogFormatter
{
    public CLIFormatter () { super(); }
    public CLIFormatter (int facility)
    {
	super (facility);
    }

    /**
     * A simple formatting routine that returns "-p ", the facility defined
     * in the base class, and the level passed in.
     *
     * @param	level	the numerical level of the message
     * @return		a string formatted for the "logger" command
     */
    public String getPrefix (int level)
    {
	// JavaLN.printStackTrace();
	return ("-p " + facility + "." + level);
    }

    /**
     * Unit tests
     */
    public static void main (String args[])
    {
    	CLIFormatter clif = new CLIFormatter();

	System.out.println (clif.format
	    (new LogRecord (Level.SEVERE, "severe")));
	System.out.println (clif.format
	    (new LogRecord (Level.FINEST, "finest")));

    	clif = new CLIFormatter (SyslogFormatter.Kern);
	System.out.println (clif.format
	    (new LogRecord (Level.SEVERE, "severe")));
	System.out.println (clif.format
	    (new LogRecord (Level.FINEST, "finest")));
    }
}
