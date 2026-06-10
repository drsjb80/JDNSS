package edu.msudenver.cs.jdnss;

final class ParsingContext {
    private String currentName;
    private int currentTTL;
    private final String origin;
    private final BindZone zone;
    private int globalTTL;
    private int SOATTL;
    private int SOAMinimumTTL;

    ParsingContext(final String origin, final BindZone zone) {
        this.origin = origin;
        this.zone = zone;
        this.currentTTL = -1;
        this.globalTTL = -1;
        this.SOATTL = -1;
        this.SOAMinimumTTL = -1;
    }

    String getCurrentName() {
        return currentName;
    }

    void setCurrentName(final String currentName) {
        this.currentName = currentName;
    }

    int getCurrentTTL() {
        return currentTTL;
    }

    void setCurrentTTL(final int currentTTL) {
        this.currentTTL = currentTTL;
    }

    String getOrigin() {
        return origin;
    }

    BindZone getZone() {
        return zone;
    }

    int getGlobalTTL() {
        return globalTTL;
    }

    void setGlobalTTL(final int globalTTL) {
        this.globalTTL = globalTTL;
    }

    int getSOATTL() {
        return SOATTL;
    }

    void setSOATTL(final int SOATTL) {
        this.SOATTL = SOATTL;
    }

    int getSOAMinimumTTL() {
        return SOAMinimumTTL;
    }

    void setSOAMinimumTTL(final int SOAMinimumTTL) {
        this.SOAMinimumTTL = SOAMinimumTTL;
    }
}
