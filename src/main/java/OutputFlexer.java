import freditor.FlexerState;

public class OutputFlexer extends Flexer {
    public static final OutputFlexer instance = new OutputFlexer();

    @Override
    public int pickColorForLexeme(FlexerState previousState, FlexerState endState) {
        return super.pickColorForLexeme(FlexerState.EMPTY, endState);
    }
}
