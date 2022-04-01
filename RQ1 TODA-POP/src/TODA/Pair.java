package src.TODA;

public class Pair<U, V> {
    public final U key; // 2^256 bits address
    public final V value; // hash
    public Pair(U key, V value) {
        this.key = key;
        this.value = value;
    }
}