package org.unicode.cldr.util;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.Locale;

/**
 * Provide a set of code point abbreviations. Includes conversions to and from codepoints, including
 * hex. Typicaly To test whether a string could have escapes, use either:
 *
 * <ul>
 *   <li>
 */
public enum CodePointEscaper {
    // These are characters found in CLDR data fields
    // The long names don't necessarily match the formal Unicode names
    TAB(9, "tab"),
    LF(0xA, "line feed"),
    CR(0xD, "carriage return"),

    // Spaces
    SP(0x20, "space", "ASCII space"),
    TSP(0x2009, "thin space", "Aka ‘narrow space’"),

    // No-break versions
    NBSP(0xA0, "no-break space", "Same as space, but doesn’t line wrap."),
    NBTSP(
            0x202F,
            "no-break thin space",
            "Same as thin space, but doesn’t line wrap. Aka 'narrow no-break space'"),

    // Line Break control
    WNJ(
            0x200B,
            "allow line wrap after, aka ZWSP",
            "Invisible character allowing a line-wrap afterwards. Also known as ‘ZWSP’."),
    WJ(
            0x2060,
            "prevent line wrap",
            "Keeps adjacent characters from line-wrapping. Also known as ‘word-joiner’."),
    SHY(
            0x00AD,
            "soft hyphen",
            "Invisible character allowing a line-wrap afterwards, but appears like a hyphen in most languages."),

    ZWNJ(0x200C, "cursive non-joiner", "Breaks cursive connections, where possible."),
    ZWJ(0x200D, "cursive joiner", "Forces cursive connections, if possible."),

    ALM(
            0x061C,
            "Arabic letter mark",
            "For BIDI, invisible character that behaves like Arabic letter."),
    LRM(
            0x200E,
            "left-right mark",
            "For BIDI, invisible character that behaves like Hebrew letter."),
    RLM(0x200F, "right-left mark", "For BIDI, invisible character that behaves like Latin letter."),

    LRO(0x202D, "left-right override"),
    RLO(0x202E, "right-left override"),
    PDF(0x202C, "end override"),

    BOM(0xFEFF, "byte-order mark"),

    ANS(0x0600, "Arabic number sign"),
    ASNS(0x0601, "Arabic sanah sign"),
    AFM(0x602, "Arabic footnote marker"),
    ASFS(0x603, "Arabic safha sign"),
    SAM(0x70F, "Syriac abbreviation mark"),
    KIAQ(0x17B4, "Khmer inherent aq"),
    KIAA(0x17B5, "Khmer inherent aa"),

    RANGE('➖', "range syntax mark", "heavy minus sign"),
    ESCS('❰', "escape start", "heavy open angle bracket"),
    ESCE('❱', "escape end", "heavy close angle bracket");

    // Alternates: Thin vs Narrow, order of NB vs those
    public static final CodePointEscaper NSP = TSP;
    public static final CodePointEscaper NBNSP = NBTSP;
    public static final CodePointEscaper NNBSP = NBTSP;
    public static final CodePointEscaper TNBSP = NBTSP;
    public static final CodePointEscaper ZWSP = WNJ;

    public static final char RANGE_SYNTAX = (char) RANGE.getCodePoint();
    public static final char ESCAPE_START = (char) ESCS.getCodePoint();
    public static final char ESCAPE_END = (char) ESCE.getCodePoint();

    /** Assemble the reverse mapping */
    private static final UnicodeMap<CodePointEscaper> _fromCodePoint = new UnicodeMap<>();

    static {
        for (CodePointEscaper abbr : CodePointEscaper.values()) {
            CodePointEscaper oldValue = _fromCodePoint.get(abbr.codePoint);
            if (oldValue != null) {
                throw new IllegalArgumentException(
                        "Abbreviation code points collide: "
                                + oldValue.name()
                                + ", "
                                + abbr.name());
            }
            _fromCodePoint.put(abbr.codePoint, abbr);
        }
        _fromCodePoint.freeze();
    }

    /** Characters that need escaping */
    public static final UnicodeSet EMOJI_INVISIBLES =
            new UnicodeSet("[\\uFE0F\\U000E0020-\\U000E007F]").freeze();

    public static final UnicodeSet FORCE_ESCAPE =
            new UnicodeSet("[[:DI:][:Pat_WS:][:WSpace:][:C:][:Z:]\u200B\u2060]")
                    .addAll(getNamedEscapes())
                    .removeAll(EMOJI_INVISIBLES)
                    .freeze();

    /** set to be escaped in the surveytool */
    public static final UnicodeSet ESCAPE_IN_SURVEYTOOL =
            FORCE_ESCAPE.cloneAsThawed().remove(SP.getCodePoint()).freeze();

    public static final UnicodeSet NON_SPACING = new UnicodeSet("[[:Mn:][:Me:]]").freeze();

    public static final UnicodeSet FORCE_ESCAPE_WITH_NONSPACING =
            new UnicodeSet(FORCE_ESCAPE).addAll(NON_SPACING).freeze();

    private final int codePoint;
    private final String shortName;
    private final String description;

    private CodePointEscaper(int codePoint, String shortName) {
        this(codePoint, shortName, "");
    }

    private CodePointEscaper(int codePoint, String shortName, String description) {
        this.codePoint = codePoint;
        this.shortName = shortName;
        this.description = description;
    }

    public static final UnicodeSet getNamedEscapes() {
        return _fromCodePoint.keySet().freeze();
    }

    /**
     * Return long names for this character. The set is immutable and ordered, with the first name
     * being the most user-friendly.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Return a longer description, if available; otherwise ""
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /** Return the code point for this character. */
    public int getCodePoint() {
        return codePoint;
    }

    /** Return the string form of the code point for this character. */
    public String getString() {
        return UTF16.valueOf(codePoint);
    }

    /** Returns the escaped form from the code point for this enum */
    public String codePointToEscaped() {
        return ESCAPE_START + rawCodePointToEscaped(codePoint) + ESCAPE_END;
    }

    /** Returns a code point from the escaped form <b>of a single code point</b> */
    public static int escapedToCodePoint(String value) {
        if (value == null || value.isEmpty()) {
            return 0xFFFD;
        }
        if (value.codePointAt(0) != CodePointEscaper.ESCAPE_START
                || value.codePointAt(value.length() - 1) != CodePointEscaper.ESCAPE_END) {
            throw new IllegalArgumentException(
                    "Must be of the form "
                            + CodePointEscaper.ESCAPE_START
                            + "…"
                            + CodePointEscaper.ESCAPE_END);
        }
        return rawEscapedToCodePoint(value.substring(1, value.length() - 1));
    }

    /** Returns the escaped form from a code point */
    public static String codePointToEscaped(int codePoint) {
        return ESCAPE_START + rawCodePointToEscaped(codePoint) + ESCAPE_END;
    }

    /** Returns the escaped form from a string */
    public static String toEscaped(String unescaped) {
        return toEscaped(unescaped, FORCE_ESCAPE);
    }

    /** Returns the escaped form from a string */
    public static String toEscaped(String unescaped, UnicodeSet toEscape) {
        if (unescaped == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        unescaped
                .codePoints()
                .forEach(
                        cp -> {
                            if (!toEscape.contains(cp)) {
                                result.appendCodePoint(cp);
                            } else {
                                result.append(codePointToEscaped(cp));
                            }
                        });
        return result.toString();
    }

    public static String getEscaped(int cp, UnicodeSet toEscape) {
        if (!toEscape.contains(cp)) {
            return UTF16.valueOf(cp);
        } else {
            return codePointToEscaped(cp);
        }
    }

    /** Return unescaped string */
    public static String toUnescaped(String escaped) {
        if (escaped == null) {
            return null;
        }
        StringBuilder result = null;
        int donePart = 0;
        int found = escaped.indexOf(ESCAPE_START);
        while (found >= 0) {
            int foundEnd = escaped.indexOf(ESCAPE_END, found);
            if (foundEnd < 0) {
                throw new IllegalArgumentException(
                        "Malformed escaped string, missing: " + ESCAPE_END);
            }
            if (result == null) {
                result = new StringBuilder();
            }
            result.append(escaped, donePart, found);
            donePart = ++foundEnd;
            result.appendCodePoint(escapedToCodePoint(escaped.substring(found, foundEnd)));
            found = escaped.indexOf(ESCAPE_START, foundEnd);
        }
        return donePart == 0
                ? escaped
                : result.append(escaped, donePart, escaped.length()).toString();
    }

    private static final String HAS_NAME = " ≡ ";

    public static String toExample(int codePoint) {
        CodePointEscaper cpe = forCodePoint(codePoint);
        if (cpe == null) { // hex
            final String name = UCharacter.getExtendedName(codePoint);
            return codePointToEscaped(codePoint)
                    + HAS_NAME
                    + (name != null ? name.toLowerCase() : "");
        } else {
            return CodePointEscaper.codePointToEscaped(cpe.codePoint)
                    + HAS_NAME
                    + cpe.shortName; // TODO show hover with cpe.description
        }
    }

    static CodePointEscaper forCodePoint(int codePoint) {
        return _fromCodePoint.get(codePoint);
    }

    static CodePointEscaper forCodePoint(String str) {
        return forCodePoint(str.codePointAt(0));
    }

    /**
     * Returns a code point from an abbreviation string or hex string <b>without the escape
     * brackets</b>
     */
    public static int rawEscapedToCodePoint(CharSequence value) {
        if (value == null || value.length() == 0) {
            return 0xFFFD;
        }
        try {
            return valueOf(value.toString().toUpperCase(Locale.ROOT)).codePoint;
        } catch (Exception e) {
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(value.toString(), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a named or hex escape: ❰" + value + "❌❱");
        }
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw new IllegalArgumentException("Illegal code point: ❰" + value + "❌❱");
        }
        return codePoint;
    }

    /**
     * Returns an abbreviation string or hex string <b>without the escape brackets</b> from a code
     * point.
     */
    public static String rawCodePointToEscaped(int codePoint) {
        CodePointEscaper result = CodePointEscaper._fromCodePoint.get(codePoint);
        return result == null
                ? Integer.toString(codePoint, 16).toUpperCase(Locale.ROOT)
                : result.toString();
    }

    public static final String getHtmlRows(
            UnicodeSet escapesToShow, String tableOptions, String cellOptions) {
        if (!escapesToShow.strings().isEmpty()) {
            throw new IllegalArgumentException("No strings allowed in the unicode set.");
        }
        StringBuilder result = new StringBuilder("<table" + tableOptions + ">");
        UnicodeSet remaining = new UnicodeSet(escapesToShow);
        String tdPlus = "<td" + cellOptions + ">";
        for (CodePointEscaper cpe : CodePointEscaper.values()) {
            int cp = cpe.getCodePoint();
            remaining.remove(cp);
            if (escapesToShow.contains(cpe.getCodePoint())) {
                final String id = cpe.name();
                final String shortName = cpe.getShortName();
                final String description = cpe.getDescription();
                addREsult(result, tdPlus, id, shortName, description);
            }
        }
        for (String cps : remaining) {
            int cp = cps.codePointAt(0);
            final String extendedName = UCharacter.getExtendedName(cp);
            addREsult(
                    result,
                    tdPlus,
                    Utility.hex(cp, 2),
                    "",
                    extendedName == null ? "" : extendedName.toLowerCase());
        }
        return result.append("</table>").toString();
    }

    public static void addREsult(
            StringBuilder result,
            String tdPlus,
            final String id,
            final String shortName,
            final String description) {
        result.append("<tr>")
                .append(tdPlus)
                .append(ESCAPE_START)
                .append(id)
                .append(ESCAPE_END + "</td>")
                .append(tdPlus)
                .append(shortName)
                .append("</td>")
                .append(tdPlus)
                .append(description)
                .append("</td><tr>");
    }
}
