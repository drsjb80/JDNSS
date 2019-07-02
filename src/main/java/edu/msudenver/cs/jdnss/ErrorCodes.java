package edu.msudenver.cs.jdnss;

import lombok.Getter;

enum ErrorCodes {
    NOERROR(0),
    FORMERROR(1),
    SERVFAIL(2),
    NAMEERROR(3),
    NOTIMPL(4),
    REFUSED(5),
    YXRRSET(7);

    @Getter
    final
    int code;

    ErrorCodes(int code) {
        this.code = code;
    }
}
