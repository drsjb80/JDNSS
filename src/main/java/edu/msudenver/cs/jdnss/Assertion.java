package edu.msudenver.cs.jdnss;
import java.lang.Exception;

class AssertionException extends Exception
{
    public AssertionException (String message)
    {
        super (message);
    }

    public AssertionException (String message, Throwable cause) 
    {
        super (message, cause);
    }

    public AssertionException (Throwable cause) 
    {
        super (cause);
    }
}

class Assertion
{
    /**
     * an Assert that is independent of version and always executes...
     *
     * @param assertion        what to test
     */
    public static void Assert (boolean assertion)
    {
        if (!assertion) throw new AssertionError ("Assertion failed");
    }

    public static void Assert (boolean assertion, String message)
    {
        if (!assertion) throw new AssertionError (message);
    }

    public static void Assert (boolean assertion, String message,
        Throwable cause)
    {
        if (!assertion) throw new AssertionError (message, cause);
    }

    public static void Assert (boolean assertion, Throwable cause)
    {
        if (!assertion) throw new AssertionError (cause);
    }
}
