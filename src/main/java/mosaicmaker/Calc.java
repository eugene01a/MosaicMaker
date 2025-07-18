package mosaicmaker;

public class Calc {

    public static int multiplyAndRound(Number a, Number b) {
        return (int) Math.round(a.doubleValue() * b.doubleValue());
    }

    public static int divideAndRound(Number a, Number b) {
        if (b.doubleValue() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return (int) Math.round(a.doubleValue() / b.doubleValue());
    }
}

