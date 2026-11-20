package com.hitorro.example.golden;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A minimal JSON reader/writer (no Jackson, so the golden test compiles against both Jackson
 * versions) that rewrites a document with every object's keys sorted. Scalars are copied as
 * written. Anything that is not JSON is returned unchanged.
 */
final class Canonical {

    private final String s;
    private int i;

    private Canonical(String s) {
        this.s = s;
    }

    static String sortKeys(String json) {
        try {
            Canonical c = new Canonical(json);
            c.ws();
            StringBuilder sb = new StringBuilder();
            c.value(sb);
            c.ws();
            return c.i == json.length() ? sb.toString() : json;
        } catch (RuntimeException e) {
            return json;
        }
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private void value(StringBuilder out) {
        char ch = s.charAt(i);
        if (ch == '{') {
            i++;
            Map<String, String> members = new TreeMap<>();
            ws();
            if (s.charAt(i) == '}') {
                i++;
                out.append("{}");
                return;
            }
            while (true) {
                ws();
                String key = string();
                ws();
                if (s.charAt(i++) != ':') throw new IllegalStateException();
                ws();
                StringBuilder v = new StringBuilder();
                value(v);
                members.put(key, v.toString());
                ws();
                char c = s.charAt(i++);
                if (c == '}') break;
                if (c != ',') throw new IllegalStateException();
            }
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, String> e : members.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(e.getKey()).append(':').append(e.getValue());
            }
            out.append('}');
        } else if (ch == '[') {
            i++;
            List<String> items = new ArrayList<>();
            ws();
            if (s.charAt(i) == ']') {
                i++;
                out.append("[]");
                return;
            }
            while (true) {
                ws();
                StringBuilder v = new StringBuilder();
                value(v);
                items.add(v.toString());
                ws();
                char c = s.charAt(i++);
                if (c == ']') break;
                if (c != ',') throw new IllegalStateException();
            }
            out.append('[').append(String.join(",", items)).append(']');
        } else if (ch == '"') {
            out.append(string());
        } else {
            int start = i;
            while (i < s.length() && ",}] \t\r\n".indexOf(s.charAt(i)) < 0) i++;
            out.append(s, start, i);
        }
    }

    /** A string token, returned as written (quotes and escapes included). */
    private String string() {
        int start = i;
        if (s.charAt(i++) != '"') throw new IllegalStateException();
        while (s.charAt(i) != '"') {
            if (s.charAt(i) == '\\') i++;
            i++;
        }
        i++;
        return s.substring(start, i);
    }
}
