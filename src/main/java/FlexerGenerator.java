public class FlexerGenerator {
    public static void main(String[] args) {
        new freditor.FlexerGenerator(-8, 9).generateTokens(
                "(", ")", "[", "]", "{", "}"
        );
    }
}
