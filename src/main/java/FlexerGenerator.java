public class FlexerGenerator {
    public static void main(String[] args) {
        new freditor.FlexerGenerator(-9, 9)
                .withIdentifierCall("symbol(input)")
                .generateTokens(
                        "false", "nil", "true",
                        "(", ")", "[", "]", "{", "}"
                );
    }
}
