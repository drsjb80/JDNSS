package edu.msudenver.cs.jdnss;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
class HINFORR extends RR {
    private final String CPU;
    private final String OS;

    HINFORR(final String name, final int ttl, final String CPU, final String OS) {
        super(name, RRCode.HINFO, ttl);
        this.CPU = CPU;
        this.OS = OS;
    }

    @Override
    protected byte[] getBytes() {
        return Utils.combine(Utils.toCS(CPU), Utils.toCS(OS));
    }
}
