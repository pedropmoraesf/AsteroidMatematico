package br.grassinimoraes.divasteroides;

import java.util.Random;

/**
 * Regras centrais do jogo. Mantem a matematica separada da apresentacao para
 * facilitar ajustes futuros sem espalhar numeros magicos pelas entidades.
 */
final class GameRules {
    static final int MIN_METEOR_VALUE = 2;
    static final int MAX_METEOR_VALUE = 200;
    static final int MAX_ODD_METEOR_VALUE = 31;
    static final int[] PRIME_AMMO = {2, 3, 5, 7, 11, 13};

    private static int difficulty = 2;

    private GameRules() {
    }

    static void setDifficulty(int level) {
        difficulty = Math.max(0, Math.min(5, level));
    }

    static int getDifficulty() {
        return difficulty;
    }

    static String getDifficultyName() {
        switch (difficulty) {
            case 0: return "Muito Facil";
            case 1: return "Facil";
            case 2: return "Medio";
            case 3: return "Dificil";
            case 4: return "Muito Dificil";
            default: return "Insano";
        }
    }

    static float getMeteorSpeedMultiplier() {
        switch (difficulty) {
            case 0: return 0.72f;
            case 1: return 0.86f;
            case 2: return 1.00f;
            case 3: return 1.15f;
            case 4: return 1.32f;
            default: return 1.52f;
        }
    }

    static boolean isAllowedMeteorValue(int value) {
        if (value < MIN_METEOR_VALUE || value > MAX_METEOR_VALUE) {
            return false;
        }
        if (value > MAX_ODD_METEOR_VALUE && (value & 1) != 0) {
            return false;
        }
        return isFullyFactorableByAmmo(value);
    }

    static boolean isFullyFactorableByAmmo(int value) {
        int remaining = value;
        for (int prime : PRIME_AMMO) {
            while (remaining % prime == 0) {
                remaining /= prime;
            }
        }
        return remaining == 1;
    }

    static boolean canDivide(int value, int divisor) {
        return divisor > 1 && value % divisor == 0;
    }

    static int primeFactorCount(int value) {
        int remaining = Math.max(1, value);
        int count = 0;
        for (int prime : PRIME_AMMO) {
            while (remaining % prime == 0) {
                remaining /= prime;
                count++;
            }
        }
        return count;
    }

    static int generateMeteorValue(Random random, int destroyedCount) {
        int progress = Math.min(5, destroyedCount / 8);
        int target = 1 + Math.min(4, progress + difficulty / 2);
        int minFactors = Math.max(1, target - 1);
        int maxFactors = Math.min(5, target + 1);
        int maxPrimeIndex = Math.min(PRIME_AMMO.length - 1, 2 + destroyedCount / 12 + difficulty / 2);

        for (int attempt = 0; attempt < 120; attempt++) {
            int factorCount = minFactors + random.nextInt(maxFactors - minFactors + 1);
            int value = 1;

            for (int i = 0; i < factorCount; i++) {
                int[] possible = new int[PRIME_AMMO.length];
                int possibleCount = 0;
                for (int p = 0; p <= maxPrimeIndex; p++) {
                    int prime = PRIME_AMMO[p];
                    if (value <= MAX_METEOR_VALUE / prime) {
                        possible[possibleCount++] = prime;
                    }
                }
                if (possibleCount == 0) {
                    break;
                }
                value *= possible[random.nextInt(possibleCount)];
            }

            if (isAllowedMeteorValue(value)) {
                return value;
            }
        }

        // Valor de seguranca: composto, didatico e totalmente fatoravel pelas municoes.
        return 30;
    }

    static float meteorScaleFor(int value) {
        float normalized = (float) Math.sqrt((value - MIN_METEOR_VALUE) /
                (float) (MAX_METEOR_VALUE - MIN_METEOR_VALUE));
        return 3.2f + normalized * 5.8f;
    }

    static float cityDamageFor(int value) {
        float normalized = (float) Math.sqrt(value / (float) MAX_METEOR_VALUE);
        return 4.0f + 14.0f * normalized;
    }

    static int scoreForMeteor(int originalValue, int wrongShots, boolean usedBonus) {
        if (usedBonus) {
            return 0;
        }
        int factors = Math.max(1, primeFactorCount(originalValue));
        int base = factors * 12 + Math.max(0, originalValue / 20);
        int perfectBonus = wrongShots == 0 ? factors * 6 : 0;
        return Math.max(5, base + perfectBonus - wrongShots * 5);
    }

    static float itemDropChance(float cityHealth) {
        float missingHealth = Math.max(0f, 100f - cityHealth);
        return Math.min(0.34f, 0.10f + missingHealth / 400f);
    }
}
