package mosaicmaker;

import java.util.List;

public class Fraction {
    private final List<Integer> numerators;
    private final List<Integer> denominators;

    public Fraction(List<Integer> numerators, List<Integer> denominators) {
        this.numerators = numerators;
        this.denominators = denominators;
    }

    public List<Integer> getNumerators() {
        return numerators;
    }

    public List<Integer> getDenominators() {
        return denominators;
    }

    public void addNumerators(Integer... newNumerators) {
        for (Integer num : newNumerators) {
            numerators.add(num);
        }
    }

    public void addDenominators(Integer... newDenominators) {
        for (Integer den : newDenominators) {
            if (den == 0) {
                throw new IllegalArgumentException("Denominator cannot be zero.");
            }
            denominators.add(den);
        }
    }

    /**
     * Calculates: (product of numerators) / (product of denominators)
     * Returns Double.NaN if either list is empty.
     */
    public double evaluateProductFraction() {
        if (numerators.isEmpty() || denominators.isEmpty()) {
            return Double.NaN;
        }

        double numeratorProduct = 1.0;
        for (int num : numerators) {
            numeratorProduct *= num;
        }

        double denominatorProduct = 1.0;
        for (int den : denominators) {
            denominatorProduct *= den;
        }

        return numeratorProduct / denominatorProduct;
    }

    public double addNumeratorAndEvaluate(int numerator) {
        numerators.add(numerator);
        return evaluateProductFraction();
    }
}
