package edu.msudenver.cs.jdnss;

class AssertionException extends Exception
{
    AssertionException(final String message)
    {
        super(message);
    }

    AssertionException(final String message, final Throwable cause) 
    {
        super(message, cause);
    }

    AssertionException(final Throwable cause) 
    {
        super(cause);
    }
}
