package edu.msudenver.cs.jdnss;

class DnssecValidationException extends Exception {
    DnssecValidationException(final String message) {
        super(message);
    }

    DnssecValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
