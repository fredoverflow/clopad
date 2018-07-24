public class FlexerGenerator {
    public static void main(String[] args) {
        new freditor.FlexerGenerator(-15, 9)
                .withIdentifierCall("symbol(input)")
                .generateTokens(
                        "false", "nil", "true"
                );
    }
}
