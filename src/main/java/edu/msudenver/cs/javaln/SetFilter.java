package edu.msudenver.cs.javaln;

import java.io.*;
import java.util.logging.*;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Print only messages from a set of classes
 */

abstract class SetFilter implements Filter
{
    private final HashSet set = new HashSet();

    /**
     * Add a comma separated list of classes to log.  If there is only one,
     * don't use any commas.
     */
    public void add (String s)
    {
	if (s != null)
	{
	    String names[] = s.split (",");

	    for (int i = 0; i < names.length; i++)
	    {
		// System.out.println ("adding: " + names[i].trim());
	        set.add ((Object) names[i].trim());
	    }
	}
    }

    /**
     * Add a comma separated list of classes to stop logging.  If there is
     * only one, don't use any commas.
     */
    public void remove (String s)
    {
	if (s != null)
	{
	    String names[] = s.split (",");

	    for (int i = 0; i < names.length; i++)
	    {
	        set.remove (names[i].trim());
	    }
	}
    }

    public String toString()
    {
	String s = getClass().getName() + " = ";

	for (Iterator i = set.iterator(); i.hasNext() ;)
	{
	    String l = (String) i.next();
	    s += l.toString() + " ";
	}

	return (s);
    }

    protected boolean isLoggable (String s)
    {
	/*
	for (Iterator i = set.iterator(); i.hasNext() ;)
	{
	    String l = (String) i.next();
	    System.out.println (l);
	    System.out.println (s);
	    System.out.println (l.equals (s));
	}
	*/
	return (set.contains (s));
    }
}

class ClassFilter extends SetFilter
{
    public ClassFilter ()
    {
	super();
	add (LogManager.getLogManager().getProperty
	    ("edu.msudenver.cs.javaln.ClassFilter.names"));
    }

    /**
     * This constructor looks for classes to log from the logging property
     * "edu.msudenver.cs.javaln.ClassFilter.names" and the String passed to it,
     * both of which it takes to be a comma separated list of class names.
     */
    public ClassFilter (String classes)
    {
	this();
        add (classes);
    };

    public boolean isLoggable (LogRecord rec)
    {
        return (isLoggable (rec.getSourceClassName()));
    }

    public static void main (String args[])
    {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel (Level.ALL);

	Logger logger = Logger.getLogger ("global");

        logger.addHandler (ch);
        logger.setUseParentHandlers (false);
	logger.setLevel (Level.ALL);

	ClassFilter mf = new ClassFilter ("edu.msudenver.cs.javaln.ClassFilter");
	System.out.println (mf);
	logger.setFilter (mf);
	logger.info ("info");
	logger.severe ("severe");

	mf = new ClassFilter ("dont.log.this.one");
	System.out.println (mf);
	logger.setFilter (mf);
	logger.info ("info");
	logger.severe ("severe");
    }
}

class MethodFilter extends SetFilter
{
    public MethodFilter ()
    {
	super();
	add (LogManager.getLogManager().getProperty
	    ("edu.msudenver.cs.javaln.MethodFilter.names"));
    }

    /**
     * This constructor looks for classes to log from the logging property
     * "edu.msudenver.cs.javaln.MethodFilter.names" and the String passed to it,
     * both of which it takes to be a comma separated list of class names.
     */
    public MethodFilter (String classes)
    {
	this();
        add (classes);
    };

    public boolean isLoggable (LogRecord rec)
    {
        return (isLoggable (rec.getSourceClassName() + "." +
	    rec.getSourceMethodName()));
    }

    public static void main (String args[])
    {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel (Level.ALL);

	Logger logger = Logger.getLogger ("global");

        logger.addHandler (ch);
        logger.setUseParentHandlers (false);
	logger.setLevel (Level.ALL);

	MethodFilter mf =
	    new MethodFilter ("edu.msudenver.cs.javaln.MethodFilter.main");
	System.out.println (mf);
	logger.setFilter (mf);
	logger.info ("info");
	logger.severe ("severe");

	mf = new MethodFilter ("dont.log.this.one");
	System.out.println (mf);
	logger.setFilter (mf);
	logger.info ("info");
	logger.severe ("severe");
    }
}
