package com.gpstracker.report.web;

/** Minimal JSON helper - just enough to emit the report responses safely. */
public final class Json {

    /** Quote and escape a string, or emit the literal null. */
    public static String s(String v) {
        if (v == null) return "null";
        StringBuilder b = new StringBuilder(v.length() + 2);
        b.append('"');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    private Json() {}
}
