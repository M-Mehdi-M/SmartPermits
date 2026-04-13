package project.smartpermits;

import android.content.Context;

public class CurrencyHelper {

    public static String format(Context ctx, double usdAmount) {
        String symbol = ctx.getString(R.string.currency_symbol);
        double rate;
        try {
            rate = Double.parseDouble(ctx.getString(R.string.currency_rate));
        } catch (NumberFormatException e) {
            rate = 1.0;
        }
        double converted = usdAmount * rate;
        String amountStr;
        if (converted == Math.floor(converted) && converted < 1_000_000) {
            amountStr = String.format("%.0f", converted);
        } else {
            amountStr = String.format("%.2f", converted);
        }

        // symbol ending with space means "symbol before amount" (e.g. "lei " → "lei 2250")
        if (symbol.endsWith(" ")) {
            return symbol + amountStr;
        }
        // Symbols that go before the amount
        if ("$".equals(symbol) || "€".equals(symbol) || "£".equals(symbol) || symbol.startsWith("R$")) {
            return symbol + amountStr;
        }
        // Symbols that go after the amount (zł, ₺, ₴)
        return amountStr + " " + symbol;
    }
}
