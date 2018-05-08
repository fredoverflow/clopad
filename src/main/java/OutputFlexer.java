public class OutputFlexer extends Flexer {
    public static final OutputFlexer instance = new OutputFlexer();

    @Override
    public int pickColorForLexeme(int endState) {
        switch (endState) {
            case FUNCTION_FIRST:
                endState = IDENTIFIER_FIRST;
                break;

            case FUNCTION_NEXT:
                endState = IDENTIFIER_NEXT;
                break;
        }
        return super.pickColorForLexeme(endState);
    }
}
