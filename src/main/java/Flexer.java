public class Flexer extends freditor.Flexer {
    public static final Flexer instance = new Flexer();

    public static final int END = 0;
    public static final int ERROR = -1;

    public static final int NEWLINE = -2;
    public static final int FIRST_SPACE = -3;
    public static final int NEXT_SPACE = 1;

    public static final int COMMENT_FIRST = -4;
    public static final int COMMENT_NEXT = 2;

    public static final int CHAR_CONSTANT_BEGIN = -5;
    public static final int CHAR_CONSTANT_INSIDE = 3;
    public static final int CHAR_CONSTANT_END = 4;

    public static final int STRING_LITERAL_BEGIN = -6;
    public static final int STRING_LITERAL_ESCAPE = 5;
    public static final int STRING_LITERAL_INSIDE = 6;
    public static final int STRING_LITERAL_END = 7;

    public static final int FIRST_DIGIT = -7;
    public static final int NEXT_DIGIT = 8;

    public static final int KEYWORD_FIRST = -8;
    public static final int KEYWORD_NEXT = 9;

    public static final int IDENTIFIER_FIRST = -9;
    public static final int IDENTIFIER_NEXT = 10;

    public static final int FUNCTION_FIRST = -10;
    public static final int FUNCTION_NEXT = 11;

    // auto-generated by freditor.FlexerGenerator
    public static final int HASH = -11;
    public static final int HASH_QUESTION = 12;
    public static final int HASH_QUESTION_AT = 13;
    public static final int PERCENT = -12;
    public static final int APOSTROPHE = -13;
    public static final int OPENING_PAREN = -14;
    public static final int CLOSING_PAREN = -15;
    public static final int LESS = -16;
    public static final int LESS_EQUAL = 14;
    public static final int EQUAL = -17;
    public static final int EQUAL_EQUAL = 15;
    public static final int MORE = -18;
    public static final int MORE_EQUAL = 16;
    public static final int AT = -19;
    public static final int OPENING_BRACKET = -20;
    public static final int CLOSING_BRACKET = -21;
    public static final int CARET = -22;
    public static final int GRAVE = -23;
    public static final int OPENING_BRACE = -24;
    public static final int CLOSING_BRACE = -25;
    public static final int TILDE = -26;
    public static final int TILDE_AT = 17;

    @Override
    public int pickColorForLexeme(int endState) {
        switch (endState) {
            default:
                return 0x333333;

            case ERROR:
                return 0xff0000;

            case COMMENT_FIRST:
            case COMMENT_NEXT:
                return 0x999988;

            case STRING_LITERAL_BEGIN:
            case STRING_LITERAL_ESCAPE:
            case STRING_LITERAL_INSIDE:
            case STRING_LITERAL_END:
                return 0x00a67a;

            case FIRST_DIGIT:
            case NEXT_DIGIT:
                return 0x143dfb;

            case KEYWORD_FIRST:
            case KEYWORD_NEXT:
                return 0x990073;

            case FUNCTION_FIRST:
            case FUNCTION_NEXT:
                return 0xcc55ca;
        }
    }

    @Override
    public String synthesizeOnInsert(int state, int nextState) {
        switch (state) {
            case STRING_LITERAL_BEGIN:
                return allowsSynthesis(nextState) ? "\"" : "";

            case OPENING_PAREN:
                return allowsSynthesis(nextState) ? ")" : "";

            case OPENING_BRACKET:
                return allowsSynthesis(nextState) ? "]" : "";

            case OPENING_BRACE:
                return allowsSynthesis(nextState) ? "}" : "";
        }
        return "";
    }

    private boolean allowsSynthesis(int nextState) {
        switch (nextState) {
            case END:
            case NEWLINE:
            case FIRST_SPACE:
            case COMMENT_FIRST:
            case CLOSING_PAREN:
            case CLOSING_BRACKET:
            case CLOSING_BRACE:
                return true;

            default:
                return false;
        }
    }

    @Override
    protected int nextStateOrEnd(int currentState, char input) {
        switch (currentState) {
            default:
                throw new AssertionError("unhandled lexer state " + currentState + " for input " + input);
            case END:
            case ERROR:
            case NEWLINE:
            case CHAR_CONSTANT_END:
            case STRING_LITERAL_END:

// auto-generated by freditor.FlexerGenerator
            case HASH_QUESTION_AT:
            case PERCENT:
            case APOSTROPHE:
            case CLOSING_PAREN:
            case LESS_EQUAL:
            case EQUAL_EQUAL:
            case MORE_EQUAL:
            case AT:
            case OPENING_BRACKET:
            case CLOSING_BRACKET:
            case CARET:
            case GRAVE:
            case OPENING_BRACE:
            case CLOSING_BRACE:
            case TILDE_AT:
                switch (input) {
                    default:
                        return ERROR;

                    case '\n':
                        return NEWLINE;
                    case ' ':
                    case ',':
                        return FIRST_SPACE;

                    case ';':
                        return COMMENT_FIRST;

                    case '\\':
                        return CHAR_CONSTANT_BEGIN;
                    case '\"':
                        return STRING_LITERAL_BEGIN;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        return FIRST_DIGIT;

                    case ':':
                        return KEYWORD_FIRST;

                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                    case 'G':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case 'Y':
                    case 'Z':

                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    case 'y':
                    case 'z':

                    case '!':
                    case '*':
                    case '+':
                    case '-':
                    case '.':
                    case '/':
                    case '?':
                    case '_':
                        return IDENTIFIER_FIRST;

// auto-generated by freditor.FlexerGenerator
                    case '#':
                        return HASH;
                    case '%':
                        return PERCENT;
                    case '\'':
                        return APOSTROPHE;
                    case '(':
                        return OPENING_PAREN;
                    case ')':
                        return CLOSING_PAREN;
                    case '<':
                        return LESS;
                    case '=':
                        return EQUAL;
                    case '>':
                        return MORE;
                    case '@':
                        return AT;
                    case '[':
                        return OPENING_BRACKET;
                    case ']':
                        return CLOSING_BRACKET;
                    case '^':
                        return CARET;
                    case '`':
                        return GRAVE;
                    case '{':
                        return OPENING_BRACE;
                    case '}':
                        return CLOSING_BRACE;
                    case '~':
                        return TILDE;
                }
            case FIRST_SPACE:
            case NEXT_SPACE:
                switch (input) {
                    case ' ':
                    case ',':
                        return NEXT_SPACE;
                    default:
                        return END;
                }
            case COMMENT_FIRST:
            case COMMENT_NEXT:
                switch (input) {
                    case '\n':
                        return END;
                    default:
                        return COMMENT_NEXT;
                }
            case CHAR_CONSTANT_BEGIN:
            case CHAR_CONSTANT_INSIDE:
                return Character.isLetterOrDigit(input) ? CHAR_CONSTANT_INSIDE : CHAR_CONSTANT_END;

            case STRING_LITERAL_BEGIN:
            case STRING_LITERAL_INSIDE:
                switch (input) {
                    case '\\':
                        return STRING_LITERAL_ESCAPE;
                    default:
                        return STRING_LITERAL_INSIDE;
                    case '\"':
                        return STRING_LITERAL_END;
                }
            case STRING_LITERAL_ESCAPE:
                switch (input) {
                    default:
                        return STRING_LITERAL_INSIDE;
                    case '\n':
                        return ERROR;
                }
            case FIRST_DIGIT:
            case NEXT_DIGIT:
                switch (input) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case '.':
                    case 'X':
                    case 'x':
                    case 'A':
                    case 'a':
                    case 'B':
                    case 'b':
                    case 'C':
                    case 'c':
                    case 'D':
                    case 'd':
                    case 'E':
                    case 'e':
                    case 'F':
                    case 'f':
                    case 'N':
                    case 'n':
                    case 'R':
                    case 'r':
                    case '/':
                        return NEXT_DIGIT;
                    default:
                        return END;
                }
            case KEYWORD_FIRST:
            case KEYWORD_NEXT:
                switch (input) {
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                    case 'G':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case 'Y':
                    case 'Z':

                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    case 'y':
                    case 'z':

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':

                    case '!':
                    case '\'':
                    case '*':
                    case '+':
                    case '-':
                    case '.':
                    case '/':
                    case ':':
                    case '?':
                    case '_':
                        return KEYWORD_NEXT;

                    default:
                        return END;
                }
            case IDENTIFIER_FIRST:
            case IDENTIFIER_NEXT:
                return identifier(input);

            case OPENING_PAREN:
                switch (input) {
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                    case 'G':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case 'Y':
                    case 'Z':

                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    case 'y':
                    case 'z':

                    case '!':
                    case '\'':
                    case '*':
                    case '+':
                    case '-':
                    case '.':
                    case '/':
                    case '?':
                    case '_':
                        return FUNCTION_FIRST;

                    default:
                        return END;
                }
            case FUNCTION_FIRST:
            case FUNCTION_NEXT:
                switch (input) {
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                    case 'G':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case 'Y':
                    case 'Z':

                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    case 'y':
                    case 'z':

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':

                    case '!':
                    case '\'':
                    case '*':
                    case '+':
                    case '-':
                    case '.':
                    case '/':
                    case ':':
                    case '?':
                    case '_':
                        return FUNCTION_NEXT;

                    default:
                        return END;
                }
            case HASH:
                return operator('?', HASH_QUESTION, input);
            case HASH_QUESTION:
                return operator('@', HASH_QUESTION_AT, input);
            case LESS:
                return operator('=', LESS_EQUAL, input);
            case EQUAL:
                return operator('=', EQUAL_EQUAL, input);
            case MORE:
                return operator('=', MORE_EQUAL, input);
            case TILDE:
                return operator('@', TILDE_AT, input);
        }
    }

    @Override
    protected int identifier(char input) {
        switch (input) {
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':

            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

            case '!':
            case '\'':
            case '*':
            case '+':
            case '-':
            case '.':
            case '/':
            case ':':
            case '?':
            case '_':
                return IDENTIFIER_NEXT;

            default:
                return END;
        }
    }
}
