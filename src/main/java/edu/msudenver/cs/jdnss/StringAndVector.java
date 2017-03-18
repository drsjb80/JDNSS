package edu.msudenver.cs.jdnss;

import java.util.Vector;

public class StringAndVector
{
    private String string;
    private Vector vector;

    StringAndVector(final String s, final Vector v)
    {
        this.string = s;
        this.vector = v;
    }

    public String getString() { return string; }
    public Vector getVector() { return vector; }
}
