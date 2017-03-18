package edu.msudenver.cs.jdnss;

import java.util.Vector;

public class SandV
{
    private String string;
    private Vector v;

    SandV (String string, Vector v)
    {
        this.string = string;
        this.v = v;
    }

    public String getString() { return (string); }
    public Vector getVector() { return (v); }
}
