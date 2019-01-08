package nl.ru.convert;

class Triple<T, U, V> {

    private final T first;
    private final U second;
    private final V third;

    Triple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    T getFirst() { return first; }
    U getSecond() { return second; }
    V getThird() { return third; }
}