package co.com.juandavidg.franchise_management.infrastructure.adapters.dynamodb.helper;

import java.text.Normalizer;
import java.util.Locale;

public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(final String value) {
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }
}
