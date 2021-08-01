public class Raw {
    private final String string;

    public Raw(Object object) {
        string = object.toString();
    }

    @Override
    public String toString() {
        return string;
    }
}
