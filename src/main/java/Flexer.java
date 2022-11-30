import freditor.FlexerState;
import freditor.FlexerStateBuilder;
import freditor.persistent.ChampMap;

import static freditor.FlexerState.EMPTY;
import static freditor.FlexerState.THIS;

public class Flexer extends freditor.Flexer {
    public static final Flexer instance = new Flexer();

    private static final FlexerState COMMA = EMPTY.head();

    private static final FlexerState COMMENT_TAIL = new FlexerState('\n', null).setDefault(THIS);
    private static final FlexerState COMMENT_HEAD = COMMENT_TAIL.head();

    private static final FlexerState CHAR_CONSTANT_TAIL = new FlexerState("09AFaz", THIS);
    private static final FlexerState CHAR_CONSTANT_HEAD = new FlexerState("!?@~", CHAR_CONSTANT_TAIL).head();

    private static final FlexerState STRING_LITERAL_END = EMPTY.tail();
    private static final FlexerState STRING_LITERAL_ESCAPE = new FlexerState('\n', null);
    private static final FlexerState STRING_LITERAL_TAIL = new FlexerState('\"', STRING_LITERAL_END, '\\', STRING_LITERAL_ESCAPE).setDefault(THIS);
    private static final FlexerState STRING_LITERAL_HEAD = STRING_LITERAL_TAIL.head();

    static {
        STRING_LITERAL_ESCAPE.setDefault(STRING_LITERAL_TAIL);
    }

    private static final FlexerState NUMBER_TAIL = new FlexerState(".9AFMMNNRRXXafrrxx", THIS);
    private static final FlexerState NUMBER_HEAD = NUMBER_TAIL.head();

    static final FlexerState SYMBOL_TAIL = new FlexerState("!!$$''*+-:<?AZ__az||", THIS);
    static final FlexerState SYMBOL_HEAD = SYMBOL_TAIL.head();

    private static final FlexerState SIGN = new FlexerStateBuilder()
            .set("!!$$''*+-:<?AZ__az||", SYMBOL_TAIL)
            .set("09", NUMBER_TAIL)
            .build()
            .head();

    static final FlexerState KEYWORD_TAIL = new FlexerState("!!$$''*+-:<?AZ__az||", THIS);
    static final FlexerState KEYWORD_HEAD = KEYWORD_TAIL.head();

    private static final FlexerState START = new FlexerStateBuilder()
            .set('(', OPENING_PAREN)
            .set(')', CLOSING_PAREN)
            .set('[', OPENING_BRACKET)
            .set(']', CLOSING_BRACKET)
            .set('{', OPENING_BRACE)
            .set('}', CLOSING_BRACE)
            .set('\n', NEWLINE)
            .set(' ', SPACE_HEAD)
            .set(',', COMMA)
            .set(';', COMMENT_HEAD)
            .set('\\', CHAR_CONSTANT_HEAD)
            .set('\"', STRING_LITERAL_HEAD)
            .set("!!$$''*+-:<?AZ__az||", SYMBOL_HEAD)
            .set("09", NUMBER_HEAD)
            .set('+', SIGN)
            .set('-', SIGN)
            .set(':', KEYWORD_HEAD)
            .build()
            .verbatim(SYMBOL_TAIL, "false", "nil", "true")
            .setDefault(ERROR);

    @Override
    protected FlexerState start() {
        return START;
    }

    @Override
    public int pickColorForLexeme(FlexerState previousState, FlexerState endState) {
        Integer color = (previousState == OPENING_PAREN ? afterOpeningParen : lexemeColors).get(endState);
        return color != null ? color : 0x333333;
    }

    private static final ChampMap<FlexerState, Integer> lexemeColors = ChampMap.of(ERROR, 0xff0000)
            .put(COMMENT_HEAD, COMMENT_TAIL, 0x999988)
            .put(CHAR_CONSTANT_HEAD, CHAR_CONSTANT_TAIL, 0x00a67a)
            .put(STRING_LITERAL_HEAD, STRING_LITERAL_TAIL, STRING_LITERAL_ESCAPE, STRING_LITERAL_END, 0x00a67a)
            .put(NUMBER_HEAD, NUMBER_TAIL, 0x143dfb)
            .put(START.read("false", "nil", "true"), 0x143dfb)
            .put(KEYWORD_HEAD, KEYWORD_TAIL, 0x990073);

    private static final ChampMap<FlexerState, Integer> afterOpeningParen = lexemeColors
            .put(START.read("a", "aa", "-", "f", "fa", "fal", "fals", "n", "ni", "t", "tr", "tru"), 0xcc55ca);

    @Override
    public boolean preventInsertion(FlexerState nextState) {
        return nextState == CLOSING_PAREN
                || nextState == CLOSING_BRACKET
                || nextState == CLOSING_BRACE
                || nextState == STRING_LITERAL_END;
    }

    @Override
    public String synthesizeOnInsert(FlexerState state, FlexerState nextState) {
        if (state == OPENING_PAREN) return ")";
        if (state == OPENING_BRACKET) return "]";
        if (state == OPENING_BRACE) return "}";
        if (state == STRING_LITERAL_HEAD) return "\"";
        return "";
    }

    @Override
    public boolean arePartners(FlexerState opening, FlexerState closing) {
        if (opening == OPENING_PAREN) return (closing == CLOSING_PAREN);
        if (opening == OPENING_BRACKET) return (closing == CLOSING_BRACKET);
        if (opening == OPENING_BRACE) return (closing == CLOSING_BRACE);
        if (opening == STRING_LITERAL_HEAD) return (closing == STRING_LITERAL_END);
        return false;
    }
}
