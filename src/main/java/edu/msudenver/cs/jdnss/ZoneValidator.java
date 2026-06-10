package edu.msudenver.cs.jdnss;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

final class ZoneValidator {
    private static final Logger logger = Logger.getLogger(ZoneValidator.class.getName());

    private ZoneValidator() {
    }

    static ValidationResult validate(final RRCode rrsetType, final String rrsetName,
                                     final List<? extends RR> rrset, final Zone zone) {
        if (!zone.isDnssecEnabled()) {
            return ValidationResult.UNVERIFIED;
        }

        final List<DNSKEYRR> dnskeys = zone.getDNSKEYs();
        if (dnskeys == null || dnskeys.isEmpty()) {
            logger.fine("No DNSKEY records in zone " + zone.getName() + " for " + rrsetName);
            return ValidationResult.UNSIGNED;
        }

        final List<RRSIG> rrsigs = findRRSIGsForRRset(rrsetType, rrsetName, rrset);
        if (rrsigs.isEmpty()) {
            logger.fine("No RRSIG found for " + rrsetType + " " + rrsetName);
            return ValidationResult.UNSIGNED;
        }

        for (final RRSIG rrsig : rrsigs) {
            try {
                if (ZoneValidator.verifyRRSIG(rrsig, rrset, dnskeys)) {
                    logger.fine("DNSSEC validation PASSED for " + rrsetType + " " + rrsetName);
                    return ValidationResult.VALID;
                }
            } catch (DnssecValidationException e) {
                logger.warning("DNSSEC validation FAILED for " + rrsetType + " " + rrsetName + ": " + e.getMessage());
                return ValidationResult.INVALID;
            }
        }

        logger.warning("No valid RRSIG signatures found for " + rrsetType + " " + rrsetName);
        return ValidationResult.INVALID;
    }

    private static List<RRSIG> findRRSIGsForRRset(final RRCode rrsetType, final String rrsetName,
                                                   final List<? extends RR> allRecords) {
        final List<RRSIG> rrsigs = new ArrayList<>();

        for (final RR rr : allRecords) {
            if (rr instanceof RRSIG) {
                final RRSIG rrsig = (RRSIG) rr;
                if (rrsig.getTypeCovered() == rrsetType && rrsig.getName().equals(rrsetName)) {
                    rrsigs.add(rrsig);
                }
            }
        }

        return rrsigs;
    }

    private static boolean verifyRRSIG(final RRSIG rrsig, final List<? extends RR> rrset,
                                       final List<DNSKEYRR> dnskeys) throws DnssecValidationException {
        final List<RR> rrsetWithoutRRSIG = new ArrayList<>();
        for (final RR rr : rrset) {
            if (!(rr instanceof RRSIG)) {
                rrsetWithoutRRSIG.add(rr);
            }
        }

        try {
            return DnssecValidator.validate(rrsig, rrsetWithoutRRSIG, dnskeys);
        } catch (DnssecValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DnssecValidationException("Unexpected error during DNSSEC validation", e);
        }
    }
}
