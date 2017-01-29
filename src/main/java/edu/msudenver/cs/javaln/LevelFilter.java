package edu.msudenver.cs.javaln;

import java.io.*;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;
import java.util.HashSet;
import java.util.Iterator;

/**
 *  Print only message from a set of levels, not all greater than some
 * level as is the default.
 */

public class LevelFilter implements Filter
{
    private HashSet levels = new HashSet();

    public LevelFilter ()
    {
	super();
	String levels =  LogManager.getLogManager().getProperty
            ("edu.msudenver.cs.javaln.LevelFilter.level");
	// System.out.println (levels);

	if (levels != null)
	{
	    String names[] = levels.split (",");

	    for (int i = 0; i < names.length; i++)
	    {
		String level = names[i].trim();
		// System.out.println (level);

		if (level.equalsIgnoreCase ("SEVERE")) add (Level.SEVERE);
		if (level.equalsIgnoreCase ("WARNING")) add (Level.WARNING);
		if (level.equalsIgnoreCase ("INFO")) add (Level.INFO);
		if (level.equalsIgnoreCase ("CONFIG")) add (Level.CONFIG);
		if (level.equalsIgnoreCase ("FINE")) add (Level.FINE);
		if (level.equalsIgnoreCase ("FINER")) add (Level.FINER);
		if (level.equalsIgnoreCase ("FINEST")) add (Level.FINEST);
	    }
	}
    };

    public LevelFilter (Level l)
    {
	this();
        levels.add (l);
    };

    public void add (Level l) { levels.add (l); }

    public void remove (Level l) { levels.remove (l); }

    public String toString()
    {
	String s = "LevelFilter, levels = ";

	for (Iterator i = levels.iterator(); i.hasNext() ;)
	{
	    Level l = (Level) i.next();

	    s += l.toString() + " ";
	}

	return (s);
    }

    public boolean isLoggable (LogRecord record)
    {
	return (levels.contains (record.getLevel()));
    }

    private static void testAll (Logger l)
    {
	l.severe ("severe");
	l.warning ("warning");
	l.info ("info");
	l.config ("config");
	l.fine ("fine");
	l.finer ("finer");
	l.finest ("finest");
    }

    public static void main (String args[])
    {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel (Level.ALL);
	ch.setFormatter (new NullFormatter());

	Logger l = Logger.getLogger ("LevelFilter");
        l.addHandler (ch);
        l.setUseParentHandlers (false);
	l.setLevel (Level.ALL);

	LevelFilter lf = new LevelFilter();
	l.setFilter (lf);

	System.out.println (lf);
	testAll (l);

	lf.add (Level.FINEST);
	System.out.println (lf);
	testAll (l);

	lf.add (Level.INFO);
	System.out.println (lf);
	testAll (l);

	lf.remove (Level.INFO);
	System.out.println (lf);
	testAll (l);
    }
}
