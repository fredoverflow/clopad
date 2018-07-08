public class OutputFlexer extends Flexer {
    public static final OutputFlexer instance = new OutputFlexer();

    @Override
    public int pickColorForLexeme(int previousState, char firstCharacter, int endState) {
        return super.pickColorForLexeme(Flexer.END, firstCharacter, endState);
    }
}
