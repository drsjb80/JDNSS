package edu.msudenver.cs.jdnss;

public class SandN
{
    private String string;
    private int number;

    SandN (String string, int number)
    {
        this.string = string;
        this.number = number;
    }

    public String getString() { return (string); }
    public int getNumber() { return (number); }
    public String toString()
    {
        return ("string = " + string + ", number = " + number);
    }
}
