package dev.noren.staybooking.identity.internal.domain.normalization;

import java.util.Locale;

public final class EmailNormalizer {

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
