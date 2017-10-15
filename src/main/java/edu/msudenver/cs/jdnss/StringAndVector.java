package edu.msudenver.cs.jdnss;

import java.util.Vector;

class StringAndVector
{
    private final String string;
    private final Vector vector;

    StringAndVector(final String s, final Vector v)
    {
        this.string = s;
        this.vector = v;
    }

    public String getString() { return string; }
    public Vector getVector() { return vector; }
}
