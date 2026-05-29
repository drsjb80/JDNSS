package edu.msudenver.cs.jdnss;

final class NetworkBinding {
    private final String host;
    private final int port;

    private NetworkBinding(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    static NetworkBinding fromParts(final String[] parts) {
        if (parts == null || parts.length < 3) {
            throw new IllegalArgumentException("Invalid network binding");
        }

        return new NetworkBinding(parts[1], Integer.parseInt(parts[2]));
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }
}
