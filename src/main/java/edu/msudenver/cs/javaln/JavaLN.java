package edu.msudenver.cs.javaln;

import java.util.logging.*;
import java.util.Enumeration;

// http://www.docjar.com/html/api/java/util/logging/Logger.java.html

/**
 * A class that gets all calling method names automatically, easing the use
 * of logging.
 */

public class JavaLN extends Logger
{
    private Level throwingLevel = Level.FINER;

    private void printStackTrace ()
    {
	StackTraceElement ste[] = new Throwable().getStackTrace();

	for (int i = 0; i < ste.length; i++)
	{
	    System.out.println (ste[i]);
	}
    }

    private void createNewConsoleHandler()
    {
	Handler h = new java.util.logging.ConsoleHandler();
	h.setFormatter (new LineNumberFormatter());
	addHandler (h);
	setUseParentHandlers (false);
    }

    /**
     * Set whether or not to use LineNumberFormatter for this logger.  If
     * true and there is no associated ConsoleHandler, create one, set the
     * Formatter to LineNumberFormatter, and setUseParentHandlers to false.
     * If false, find any instances of ConsoleHandler and set the formatter
     * to SimpleFormatter.  Okay, this is a little ugly, but i often want
     * to use LineNumberFormatter and this simplifies that.
     */
    public void useLineNumberFormatter (boolean use)
    {
	Handler handlers[] = getHandlers();

	if (use && handlers.length == 0)
	{
	    createNewConsoleHandler();
	    return;
	}

	boolean found = false;
	for (int i = 0; i < handlers.length; i++)
	{
	    // System.out.println ("handlers[" + i + "] = " + handlers[i]);
	    if (handlers[i] instanceof ConsoleHandler)
	    {
		Formatter f = handlers[i].getFormatter();

		if (use && ! (f instanceof LineNumberFormatter))
		{
		    found = true;
		    handlers[i].setFormatter (new LineNumberFormatter());
		}
		else if (!use && (f instanceof LineNumberFormatter))
		{
		    handlers[i].setFormatter (new SimpleFormatter());
		}
	    }
	}

	if (use && ! found)
	{
	    createNewConsoleHandler();
	}
    }

    private void copyLogger (Logger logger)
    {
	setLevel (logger.getLevel());
	setUseParentHandlers (logger.getUseParentHandlers());

	if (logger.getFilter() != null)
	    setFilter (logger.getFilter());

	if (logger.getParent() != null)
	    setParent (logger.getParent());

	Handler handlers[] = logger.getHandlers();
	for (int i = 0; i < handlers.length; i++)
	{
	    addHandler (handlers[i]);
	}
    }

    private void add (String s)
    {
	LogManager lm = LogManager.getLogManager();
	Logger logger = lm.getLogger (s);

	/*
	 * if this isn't the first instance, copy everything over.
	 */
	if (logger != null)
	{
	    copyLogger (logger);
	}
	else
	{
	    lm.addLogger (this);
	}
    }

    /**
     * Create a logger whose name is the calling class's.
     */
    public JavaLN ()
    {
	super (new Throwable().getStackTrace()[1].getClassName(), null);
	add (new Throwable().getStackTrace()[1].getClassName());
    }

    /**
     * Create a logger whose name passed in
     *
     * @param s the name of the logger.
     */
    public JavaLN (String s)
    {
        super (s, null);
	add (s);
    }

    protected JavaLN (String s, String t)
    {
        super (s, t);
	add (s);
    }

    private static Logger getOne (String s)
    {
	LogManager lm = LogManager.getLogManager();
	Logger logger = lm.getLogger (s);

	if (logger == null)
	    logger = new JavaLN (s);

	lm.addLogger (logger);
	return (logger);
    }

    /**
     * Get a logger named after the class.
     */
    public static Logger getLogger()
    {
	String s = new Throwable().getStackTrace()[1].getClassName();
	return (getOne (s));
    }

    public static Logger getLogger (String s)
    {
	return (getOne (s));
    }

    public String toString()
    {
        return (getName() + ", level: " + getLevel() +
	    ", parent: '" + getParent() + "'");
    }

    /**
     * Log entering a method without having to pass the class and method
     * names.
     */
    public void entering()
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	entering (ste.getClassName(), ste.getMethodName());
    }

    /**
     * Log entering a method without having to pass the class and method
     * names.
     *
     * @param o the Object logged along with the class and method.
     */
    public void entering (Object o)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	entering (ste.getClassName(), ste.getMethodName(), o);
    }

    private void doEnter (String s)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[2];
	entering (ste.getClassName(), ste.getMethodName(), s);
    }

    public void entering (byte b)	{ doEnter ("" + b); }
    public void entering (short s)	{ doEnter ("" + s); }
    public void entering (int i)	{ doEnter ("" + i); }
    public void entering (long l)	{ doEnter ("" + l); }
    public void entering (float f)	{ doEnter ("" + f); }
    public void entering (double d)	{ doEnter ("" + d); }
    public void entering (boolean b)	{ doEnter ("" + b); }
    public void entering (char c)	{ doEnter ("" + c); }

    /**
     * Log entering a method without having to pass the class and method
     * names.
     *
     * @param o the array of Objects logged along with the class and method.
     */
    public void entering (Object[] o)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	entering (ste.getClassName(), ste.getMethodName(), o);
    }

    /**
     * Log leaving a method without having to pass the class and method
     * names.
     */
    public void exiting()
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	exiting (ste.getClassName(), ste.getMethodName());
    }

    /**
     * Log leaving a method without having to pass the class and method
     * names.
     *
     * @param o the Object logged along with the class and method.
     */
    public void exiting (Object o)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	exiting (ste.getClassName(), ste.getMethodName(),
	    o == null ? "null" : o.toString());
    }

    private void doExit (String s)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[2];
	exiting (ste.getClassName(), ste.getMethodName(), s);
    }

    public void exiting (byte b)	{ doExit ("" + b); }
    public void exiting (short s)	{ doExit ("" + s); }
    public void exiting (int i)		{ doExit ("" + i); }
    public void exiting (long l)	{ doExit ("" + l); }
    public void exiting (float f)	{ doExit ("" + f); }
    public void exiting (double d)	{ doExit ("" + d); }
    public void exiting (boolean b)	{ doExit ("" + b); }
    public void exiting (char c)	{ doExit ("" + c); }

    public Level setThrowingLevel (Level l)
    {
	Level ret = throwingLevel;
        throwingLevel = l;
	return (ret);
    }

    /**
     * Log throwing without having to pass the class and method names.
     *
     * @param t the Throwable.
     */
    public void throwing (Throwable t)
    {
	StackTraceElement ste = new Throwable().getStackTrace()[1];
	if (throwingLevel == Level.FINER)
	    throwing (ste.getClassName(), ste.getMethodName(), t);
	else
	    logp (throwingLevel, ste.getClassName(), ste.getMethodName(),
	        "THROW", t);
    }

    private void doObject (Level level, Object o)
    {
	if (isLoggable (level))
	{
	    StackTraceElement ste = new Throwable().getStackTrace()[2];
	    logp (level, ste.getClassName(), ste.getMethodName(),
		(o == null ? "null" : o.toString()));
	}
    }

    public void severe (Object o)	{ doObject (Level.SEVERE, o); }
    public void warning (Object o)	{ doObject (Level.WARNING, o); }
    public void info (Object o)		{ doObject (Level.INFO, o); }
    public void config (Object o)	{ doObject (Level.CONFIG, o); }
    public void fine (Object o)		{ doObject (Level.FINE, o); }
    public void finer (Object o)	{ doObject (Level.FINER, o); }
    public void finest (Object o)	{ doObject (Level.FINEST, o); }

    private void doString (Level level, String s)
    {
	if (isLoggable (level))
	{
	    StackTraceElement ste = new Throwable().getStackTrace()[2];
	    logp (level, ste.getClassName(), ste.getMethodName(), s);
	}
    }

    public void severe (byte b)		{ doString (Level.SEVERE, "" + b); }
    public void warning (byte b)	{ doString (Level.WARNING, "" + b); }
    public void info (byte b)		{ doString (Level.INFO, "" + b); }
    public void config (byte b)		{ doString (Level.CONFIG, "" + b); }
    public void fine (byte b)		{ doString (Level.FINE, "" + b); }
    public void finer (byte b)		{ doString (Level.FINER, "" + b); }
    public void finest (byte b)		{ doString (Level.FINEST, "" + b); }

    public void severe (short s)	{ doString (Level.SEVERE, "" + s); }
    public void warning (short s)	{ doString (Level.WARNING, "" + s); }
    public void info (short s)		{ doString (Level.INFO, "" + s); }
    public void config (short s)	{ doString (Level.CONFIG, "" + s); }
    public void fine (short s)		{ doString (Level.FINE, "" + s); }
    public void finer (short s)		{ doString (Level.FINER, "" + s); }
    public void finest (short s)	{ doString (Level.FINEST, "" + s); }

    public void severe (int i)		{ doString ( Level.SEVERE, "" + i); }
    public void warning (int i)		{ doString ( Level.WARNING, "" + i); }
    public void info (int i)		{ doString ( Level.INFO, "" + i); }
    public void config (int i)		{ doString ( Level.CONFIG, "" + i); }
    public void fine (int i)		{ doString ( Level.FINE, "" + i); }
    public void finer (int i)		{ doString ( Level.FINER, "" + i); }
    public void finest (int i)		{ doString ( Level.FINEST, "" + i); }

    public void severe (long l)		{ doString (Level.SEVERE, "" + l); }
    public void warning (long l)	{ doString (Level.WARNING, "" + l); }
    public void info (long l)		{ doString (Level.INFO, "" + l); }
    public void config (long l)		{ doString (Level.CONFIG, "" + l); }
    public void fine (long l)		{ doString (Level.FINE, "" + l); }
    public void finer (long l)		{ doString (Level.FINER, "" + l); }
    public void finest (long l)		{ doString (Level.FINEST, "" + l); }

    public void severe (float f)	{ doString (Level.SEVERE, "" + f); }
    public void warning (float f)	{ doString (Level.WARNING, "" + f); }
    public void info (float f)		{ doString (Level.INFO, "" + f); }
    public void config (float f)	{ doString (Level.CONFIG, "" + f); }
    public void fine (float f)		{ doString (Level.FINE, "" + f); }
    public void finer (float f)		{ doString (Level.FINER, "" + f); }
    public void finest (float f)	{ doString (Level.FINEST, "" + f); }

    public void severe (double d)	{ doString (Level.SEVERE, "" + d); }
    public void warning (double d)	{ doString (Level.WARNING, "" + d); }
    public void info (double d)		{ doString (Level.INFO, "" + d); }
    public void config (double d)	{ doString (Level.CONFIG, "" + d); }
    public void fine (double d)		{ doString (Level.FINE, "" + d); }
    public void finer (double d)	{ doString (Level.FINER, "" + d); }
    public void finest (double d)	{ doString (Level.FINEST, "" + d); }

    public void severe (boolean b)	{ doString (Level.SEVERE, "" + b); }
    public void warning (boolean b)	{ doString (Level.WARNING, "" + b); }
    public void info (boolean b)	{ doString (Level.INFO, "" + b); }
    public void config (boolean b)	{ doString (Level.CONFIG, "" + b); }
    public void fine (boolean b)	{ doString (Level.FINE, "" + b); }
    public void finer (boolean b)	{ doString (Level.FINER, "" + b); }
    public void finest (boolean b)	{ doString (Level.FINEST, "" + b); }

    public void severe (char c)		{ doString (Level.SEVERE, "" + c); }
    public void warning (char c)	{ doString (Level.WARNING, "" + c); }
    public void info (char c)		{ doString (Level.INFO, "" + c); }
    public void config (char c)		{ doString (Level.CONFIG, "" + c); }
    public void fine (char c)		{ doString (Level.FINE, "" + c); }
    public void finer (char c)		{ doString (Level.FINER, "" + c); }
    public void finest (char c)		{ doString (Level.FINEST, "" + c); }

    public void severe (Throwable t) { doString (Level.SEVERE, t.toString()); }
    public void warning (Throwable t){ doString (Level.WARNING, t.toString()); }
    public void info (Throwable t)   { doString (Level.INFO, t.toString()); }
    public void config (Throwable t) { doString (Level.CONFIG, t.toString()); }
    public void fine (Throwable t)   { doString (Level.FINE, t.toString()); }
    public void finer (Throwable t)  { doString (Level.FINER, t.toString()); }
    public void finest (Throwable t) { doString (Level.FINEST, t.toString()); }

    public static Level getLevel (String s)
    {
        if (s.equalsIgnoreCase ("severe")) return (Level.SEVERE);
        if (s.equalsIgnoreCase ("warning")) return (Level.WARNING);
        if (s.equalsIgnoreCase ("info")) return (Level.INFO);
        if (s.equalsIgnoreCase ("config")) return (Level.CONFIG);
        if (s.equalsIgnoreCase ("fine")) return (Level.FINE);
        if (s.equalsIgnoreCase ("finer")) return (Level.FINER);
        if (s.equalsIgnoreCase ("finest")) return (Level.FINEST);
	return (Level.ALL);
    }

    public static void main (String args[])
    {
	ConsoleHandler ch = new ConsoleHandler();
	ch.setLevel (Level.FINEST);

        JavaLN l = new JavaLN();
	l.setLevel (Level.FINEST);
	l.addHandler (ch);
	l.setUseParentHandlers (false);

	l.info (l.toString());
	l.severe ("this is a test");
	l.entering ("not", "needed");	// check for call to base class
	l.entering();
	l.entering (new Integer (10));
	l.entering (args);
	l.entering (new Object[]{new Integer (1), "one"});
	l.exiting ();
	l.exiting ("exiting");
	l.throwing (new Throwable ("Throwable message"));

        JavaLN m = new JavaLN ("one");
	m.severe (m.toString());
	m.severe ("this is another test");

        JavaLN n = new JavaLN ("two", null);
	n.severe (n.toString());
	n.severe ("this is a third test");
	n.warning (new Throwable ("this is a test"));
    }
}
