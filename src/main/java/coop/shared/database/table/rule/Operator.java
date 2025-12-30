package coop.shared.database.table.rule;

import java.util.function.BiFunction;

public enum Operator {
    EQ(Double::equals),
    GT((m, t) -> m > t),
    LT((m, t) -> m < t),
    GTEQ((m, t) -> m >= t),
    LTEQ((m, t) -> m <= t);


    private final BiFunction<Double, Double, Boolean> comparator;
    Operator(BiFunction<Double, Double, Boolean> comparator) {
        this.comparator = comparator;
    }

    public boolean evaluate(Double metric, Double threshold) {
        if(metric == null || threshold == null) {
            return false;
        }

        return comparator.apply(metric, threshold);
    }
}
