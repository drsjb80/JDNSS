package edu.msudenver.cs.jdnss;

public class StringAndNumber
{
    private final String string;
    private final int number;

    StringAndNumber(final String s, final int n)
    {
        this.string = s;
        this.number = n;
    }

    public String getString() { return string; }
    public int getNumber() { return number; }
    public String toString()
    {
        return "string = " + string + ", number = " + number;
    }
}
