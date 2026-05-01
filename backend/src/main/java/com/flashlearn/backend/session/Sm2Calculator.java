
package com.flashlearn.backend.session;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class Sm2Calculator {

    public static final double MIN_EASE_FACTOR     = 1.3;
    public static final double DEFAULT_EASE_FACTOR = 2.5;  

    public record Sm2Result(
        double easeFactor,
        int intervalDays,
        int repetitions,
        LocalDate nextReviewDate
) {}

 /**
     * Przelicza SM-2 na podstawie quality 0–5.
     * EF z bazy jest sanityzowany przed użyciem.
     */
    public Sm2Result calculate(int quality, double rawEF,
                               int prevInterval, int repetitions) {
        double ef = sanitizeEF(rawEF);

        double newEF = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEF = Math.max(newEF, MIN_EASE_FACTOR);

        int newInterval;
        int newRepetitions;

        if (quality < 3) {
            // błędna odpowiedź → reset
            newRepetitions = 0;
            newInterval    = 1;
        } else {
            newRepetitions = repetitions + 1;
            newInterval = switch (repetitions) {
                case 0  -> 1;
                case 1  -> 6;
                default -> (int) Math.round(prevInterval * newEF);
            };
        }

        return new Sm2Result(newEF, newInterval, newRepetitions,
                LocalDate.now().plusDays(newInterval));
    }

    /**
     * Fallback dla nieprawidłowego EF:
     * ujemny / NaN / Infinity → reset do 2.5
     * za niski (< 1.3)       → clamp do 1.3 (per spec SM-2)
     */
    public double sanitizeEF(double rawEF) {
        if (Double.isNaN(rawEF) || Double.isInfinite(rawEF) || rawEF < 0) {
            return DEFAULT_EASE_FACTOR;  // fallback dla garbage values
        }
        return Math.max(rawEF, MIN_EASE_FACTOR);
    }
}
