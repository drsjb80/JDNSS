package edu.msudenver.cs.jdnss;

import java.lang.reflect.Method;

final class P38 {
    private P38() {}

    static Object call(final String methodName, final Object o)
            throws Exception {
        final Method m = o.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(o);
    }

    static Object call(final String methodName, final Object o,
                              final Object[] args) throws Exception {
        final Class[] arguments = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            arguments[i] = args[i].getClass();
        }

        final Method m = o.getClass().getDeclaredMethod(methodName, arguments);
        m.setAccessible(true);
        return m.invoke(o, args);
    }
}
