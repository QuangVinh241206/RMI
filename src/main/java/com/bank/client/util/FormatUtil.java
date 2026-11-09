package com.bank.client.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " đ";
    }

    public static String formatDateTime(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(formatter);
    }
}
