public class FlexerGenerator {
    public static void main(String[] args) {
        new freditor.FlexerGenerator(-10, 11).generateTokens(
                "(", ")", "[", "]", "{", "}"
        );
    }
}
