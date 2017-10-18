package edu.msudenver.cs.jdnss;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Vector;

class StringAndVector
{
    @Getter(AccessLevel.PACKAGE) private final String string;
    @Getter(AccessLevel.PACKAGE) private final Vector vector;

    StringAndVector(final String s, final Vector v)
    {
        this.string = s;
        this.vector = v;
    }
}
