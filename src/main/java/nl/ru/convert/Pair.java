package nl.ru.convert;

class Pair<T, U> {

    private final T first;
    private final U second;

    Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    T getFirst() { return first; }
    U getSecond() { return second; }
}
