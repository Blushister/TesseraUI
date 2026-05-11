package com.tesseraui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TesseraBindingResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TesseraBindingResolver.class);
    private static final Pattern EXPR = Pattern.compile("\\{\\{(.*?)\\}\\}");

    private TesseraBindingResolver() {}

    public static String resolve(String template, TesseraModel model) {
        if (template == null || template.isEmpty()) return template;
        var sb = new StringBuffer();
        var m = EXPR.matcher(template);
        while (m.find()) {
            String expr = m.group(1).trim();
            String value = evaluateExpr(expr, model);
            m.appendReplacement(sb, Matcher.quoteReplacement(
                value != null ? value : "{{ " + expr + " }}"
            ));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String evaluateExpr(String expr, TesseraModel model) {
        int qMark = expr.indexOf('?');
        if (qMark >= 0) {
            String cond = expr.substring(0, qMark).trim();
            String rest = expr.substring(qMark + 1);
            // Prefer " : " (with spaces) to avoid colliding with the "t:" prefix.
            int spacedColon = rest.indexOf(" : ");
            int colon;
            String trueVal;
            String falseVal;
            if (spacedColon >= 0) {
                trueVal  = stripQuotes(rest.substring(0, spacedColon).trim());
                falseVal = stripQuotes(rest.substring(spacedColon + 3).trim());
            } else {
                colon = rest.indexOf(':');
                if (colon < 0) {
                    LOGGER.warn("[TesseraUI] Ternary expression missing ':' — expression: '{}'", expr);
                    return null;
                }
                trueVal  = stripQuotes(rest.substring(0, colon).trim());
                falseVal = stripQuotes(rest.substring(colon + 1).trim());
            }
            return evaluateCondition(cond, model)
                    ? resolveTranslation(trueVal)
                    : resolveTranslation(falseVal);
        }
        return evaluateArithmetic(expr, model);
    }

    /** Resolves a {@code t:key} translation reference, or returns the string as-is. */
    private static String resolveTranslation(String s) {
        if (s != null && s.startsWith("t:")) {
            return TesseraI18n.translate(s.substring(2).trim());
        }
        return s;
    }

    private static boolean evaluateCondition(String cond, TesseraModel model) {
        for (String op : new String[]{">=", "<=", "!=", ">", "<", "=="}) {
            int idx = cond.indexOf(op);
            if (idx >= 0) {
                String left  = cond.substring(0, idx).trim();
                String right = cond.substring(idx + op.length()).trim();
                double lv = toDouble(evaluateArithmetic(left, model));
                double rv = toDouble(stripQuotes(right));
                return switch (op) {
                    case ">"  -> lv > rv;
                    case "<"  -> lv < rv;
                    case ">=" -> lv >= rv;
                    case "<=" -> lv <= rv;
                    case "==" -> lv == rv;
                    case "!=" -> lv != rv;
                    default   -> false;
                };
            }
        }
        String val = model.resolve(cond);
        return isTruthy(val);
    }

    private static String evaluateArithmetic(String expr, TesseraModel model) {
        if (expr.startsWith("t:")) {
            return TesseraI18n.translate(expr.substring(2).trim());
        }
        for (String op : new String[]{"+", "-", "*", "/"}) {
            int idx = findOperator(expr, op);
            if (idx >= 0) {
                String left  = expr.substring(0, idx).trim();
                String right = expr.substring(idx + 1).trim();
                String lv = evaluateArithmetic(left, model);
                String rv = evaluateArithmetic(right, model);
                try {
                    double result = applyOp(toDouble(lv), op, toDouble(rv));
                    return result == Math.floor(result) ? String.valueOf((long) result) : String.valueOf(result);
                } catch (Exception e) { return null; }
            }
        }
        String resolved = model.resolve(expr);
        return resolved != null ? resolved : (isNumericLiteral(expr) ? expr : null);
    }

    private static int findOperator(String expr, String op) {
        for (int i = expr.length() - 1; i >= 0; i--) {
            if (expr.charAt(i) == op.charAt(0) && expr.substring(i).startsWith(op)) {
                if (op.equals("-") && i == 0) continue;
                return i;
            }
        }
        return -1;
    }

    private static double applyOp(double l, String op, double r) {
        return switch (op) {
            case "+" -> l + r;
            case "-" -> l - r;
            case "*" -> l * r;
            case "/" -> r != 0 ? l / r : 0;
            default  -> l;
        };
    }

    private static boolean isTruthy(String value) {
        if (value == null) return false;
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true", "1", "yes", "on" -> true;
            default -> false;
        };
    }

    private static double toDouble(String value) {
        if (value == null) return 0;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static boolean isNumericLiteral(String s) {
        try { Double.parseDouble(s.trim()); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private static String stripQuotes(String s) {
        return s.replaceAll("^\"|\"$|^'|'$", "");
    }
}
