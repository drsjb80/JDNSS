package edu.msudenver.cs.javaln;

import java.io.*;
import java.util.logging.*;

/**
 * Print only the message, not the time, level, etc.
 * An example of how to use this in code:
 * <hr>
 * <code>
 * ConsoleHandler ch = new ConsoleHandler();<br>
 * ch.setFormatter (new NullFormatter());<br>
 * ch.setLevel (Level.FINEST);<br>
 * <br>
 * Logger logger = Logger.global;<br>
 * logger.addHandler (ch);<br>
 * logger.setUseParentHandlers (false);<br>
 * logger.setLevel (Level.FINEST);<br>
 * </code>
 * <hr>
 * Using a logging configuration file:
 * <hr>
 * <code>
 * java.util.logging.ConsoleHandler.formatter = edu.msudenver.cs.javaln.NullFormatter
 * </code>
 * <hr>
 */

public class NullFormatter extends SimpleFormatter
{
    /*
    public NullFormatter()
    {
        System.out.println ("in NullFormatter");
    }

    public String formatMessage (LogRecord rec)
    {
        System.out.println ("in formatMessage");
	return ("");
    }
    */

    public String format (LogRecord rec)
    {
	// return (rec.getLevel() + ": " + formatMessage(rec) +
	return (formatMessage(rec) + System.getProperty ("line.separator"));
    }

    public static void main (String args[])
    {
	ConsoleHandler ch = new ConsoleHandler();
	ch.setFormatter (new NullFormatter());
	ch.setLevel (Level.FINEST);

        Logger logger = Logger.getLogger ("global");
	logger.addHandler (ch);
	logger.setUseParentHandlers (false);
	logger.setLevel (Level.FINEST);

	logger.severe ("this is a test");
	logger.finest ("this is another");
    }
}
