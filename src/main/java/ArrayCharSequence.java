public class ArrayCharSequence implements CharSequence {
    private final char[] array;
    private final int offset;
    private final int length;

    public ArrayCharSequence(char[] array, int offset, int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return array[offset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new ArrayCharSequence(array, offset + start, end - start);
    }

    @Override
    public String toString() {
        return new String(array, offset, length);
    }
}
