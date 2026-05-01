package com.flashlearn.backend.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class Sm2CalculatorTest {

    private Sm2Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Sm2Calculator();
    }

    // ─── sanitizeEF ───────────────────────────────────────────────

    @Test
    @DisplayName("sanitizeEF: ujemny EF → reset do 2.5")
    void sanitizeEF_negative_returnsDefault() {
        assertThat(calculator.sanitizeEF(-1.0)).isEqualTo(2.5);
        assertThat(calculator.sanitizeEF(-0.001)).isEqualTo(2.5);
    }

    @Test
    @DisplayName("sanitizeEF: NaN → reset do 2.5")
    void sanitizeEF_nan_returnsDefault() {
        assertThat(calculator.sanitizeEF(Double.NaN)).isEqualTo(2.5);
    }

    @Test
    @DisplayName("sanitizeEF: Infinity → reset do 2.5")
    void sanitizeEF_infinity_returnsDefault() {
        assertThat(calculator.sanitizeEF(Double.POSITIVE_INFINITY)).isEqualTo(2.5);
        assertThat(calculator.sanitizeEF(Double.NEGATIVE_INFINITY)).isEqualTo(2.5);
    }

    @Test
    @DisplayName("sanitizeEF: za niski (< 1.3) → clamp do 1.3")
    void sanitizeEF_tooLow_clampsToMinimum() {
        assertThat(calculator.sanitizeEF(0.5)).isEqualTo(1.3);
        assertThat(calculator.sanitizeEF(1.0)).isEqualTo(1.3);
        assertThat(calculator.sanitizeEF(1.299)).isEqualTo(1.3);
    }

    @Test
    @DisplayName("sanitizeEF: poprawna wartość → zwraca bez zmian")
    void sanitizeEF_valid_returnsUnchanged() {
        assertThat(calculator.sanitizeEF(2.5)).isEqualTo(2.5);
        assertThat(calculator.sanitizeEF(1.3)).isEqualTo(1.3);
        assertThat(calculator.sanitizeEF(4.0)).isEqualTo(4.0);
    }

    // ─── calculate: błędne odpowiedzi (quality < 3) ──────────────

    @ParameterizedTest(name = "quality={0} → reset interwału do 1")
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("calculate: quality < 3 → reset repetitions i interval")
    void calculate_wrongAnswer_resetsInterval(int quality) {
        Sm2Calculator.Sm2Result result = calculator.calculate(quality, 2.5, 10, 5);

        assertThat(result.intervalDays()).isEqualTo(1);
        assertThat(result.repetitions()).isEqualTo(0);
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("calculate: quality < 3 → EF spada, ale nie poniżej 1.3")
    void calculate_wrongAnswer_efDoesNotDropBelowMinimum() {
        // quality=0 maksymalnie obniża EF
        Sm2Calculator.Sm2Result result = calculator.calculate(0, 1.3, 1, 0);

        assertThat(result.easeFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // ─── calculate: poprawne odpowiedzi (quality >= 3) ────────────

    @Test
    @DisplayName("calculate: pierwsza poprawna odpowiedź → interval = 1")
    void calculate_firstCorrect_intervalOne() {
        Sm2Calculator.Sm2Result result = calculator.calculate(5, 2.5, 1, 0);

        assertThat(result.intervalDays()).isEqualTo(1);
        assertThat(result.repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("calculate: druga poprawna odpowiedź → interval = 6")
    void calculate_secondCorrect_intervalSix() {
        Sm2Calculator.Sm2Result result = calculator.calculate(5, 2.5, 1, 1);

        assertThat(result.intervalDays()).isEqualTo(6);
        assertThat(result.repetitions()).isEqualTo(2);
    }

    @Test
    @DisplayName("calculate: kolejne powtórzenia → interval = prevInterval * newEF")
    void calculate_subsequentRepetitions_intervalGrows() {
        Sm2Calculator.Sm2Result result = calculator.calculate(5, 2.5, 6, 2);

        // interval musi być >= prevInterval (musi rosnąć)
        assertThat(result.intervalDays()).isGreaterThan(6);
        // i musi być wynikiem prevInterval * newEF (zaokrąglone)
        assertThat(result.intervalDays()).isEqualTo(15);
    }

    @Test
    @DisplayName("calculate: quality=5 → EF rośnie")
    void calculate_perfectAnswer_efIncreases() {
        Sm2Calculator.Sm2Result result = calculator.calculate(5, 2.5, 1, 0);

        assertThat(result.easeFactor()).isGreaterThan(2.5);
    }

    @Test
    @DisplayName("calculate: quality=3 → EF spada (trudna odpowiedź obniża EF)")
    void calculate_hardCorrect_efSlightlyDecreases() {
        Sm2Calculator.Sm2Result result = calculator.calculate(3, 2.5, 1, 0);

        // q=3: delta = 0.1 - (5-3)*(0.08 + (5-3)*0.02) = 0.1 - 2*0.12 = -0.14
        assertThat(result.easeFactor()).isLessThan(2.5);
        assertThat(result.easeFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // ─── calculate: fallback złego EF ────────────────────────────

    @Test
    @DisplayName("calculate: ujemny EF w bazie → używa 2.5, liczy poprawnie")
    void calculate_negativeEFFromDb_usesFallback() {
        Sm2Calculator.Sm2Result withBadEF  = calculator.calculate(5, -1.0, 6, 2);
        Sm2Calculator.Sm2Result withDefault = calculator.calculate(5, 2.5,  6, 2);

        // zachowuje się jak domyślny EF 2.5
        assertThat(withBadEF.intervalDays()).isEqualTo(withDefault.intervalDays());
    }

    @Test
    @DisplayName("calculate: NaN EF w bazie → używa 2.5, nie rzuca wyjątku")
    void calculate_nanEFFromDb_doesNotThrow() {
        assertThatCode(() -> calculator.calculate(5, Double.NaN, 6, 2))
                .doesNotThrowAnyException();
    }

    // ─── calculate: nextReviewDate ────────────────────────────────

    @Test
    @DisplayName("calculate: nextReviewDate = dziś + intervalDays")
    void calculate_nextReviewDate_isCorrect() {
        Sm2Calculator.Sm2Result result = calculator.calculate(5, 2.5, 6, 2);

        assertThat(result.nextReviewDate())
                .isEqualTo(LocalDate.now().plusDays(result.intervalDays()));
    }
}