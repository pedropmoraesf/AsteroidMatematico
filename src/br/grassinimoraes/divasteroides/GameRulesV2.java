package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class GameRulesV2 {
    static final int GLOBAL_MAX_METEOR_VALUE = 200;
    static final int[] PRIME_AMMO = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31};

    private GameRulesV2() {
    }

    static int indexOfPrime(int prime) {
        for (int i = 0; i < PRIME_AMMO.length; i++) {
            if (PRIME_AMMO[i] == prime) {
                return i;
            }
        }
        return -1;
    }

    static int roundMaxValue(int wave) {
        int w = Math.max(1, wave) - 1;
        return Math.min(GLOBAL_MAX_METEOR_VALUE, 18 + 5 * w + w * w);
    }

    static int allowedPrimeIndexForWave(int wave) {
        if (wave <= 1) return 1;   // 2, 3
        if (wave == 2) return 2;   // +5
        if (wave == 3) return 3;   // +7
        if (wave <= 5) return 4;   // +11
        if (wave <= 7) return 5;   // +13
        if (wave == 8) return 6;   // +17
        if (wave == 9) return 7;   // +19
        if (wave == 10) return 8;  // +23
        if (wave == 11) return 9;  // +29
        return 10;                 // +31
    }

    static int nextEligibleLockedPrime(PlayerInventoryV2 inventory, int wave) {
        int limit = allowedPrimeIndexForWave(wave);
        for (int i = 0; i <= limit; i++) {
            int prime = PRIME_AMMO[i];
            if (!inventory.isPrimeUnlocked(prime)) {
                return prime;
            }
        }
        return -1;
    }

    static int generateMeteorValue(Random random, int wave, PlayerInventoryV2 inventory) {
        int maxValue = roundMaxValue(wave);

        if (wave >= 5 && inventory.getSubtractorCharge() > 0 && random.nextFloat() < primeChallengeChance(wave)) {
            int primeChallenge = generateSolvablePrimeChallenge(random, maxValue, inventory);
            if (primeChallenge > 0) {
                return primeChallenge;
            }
        }

        int[] unlocked = inventory.getUnlockedPrimes();
        int desiredFactors = Math.min(5, 2 + Math.max(0, wave - 1) / 3);

        for (int attempt = 0; attempt < 160; attempt++) {
            int factorCount = 1 + random.nextInt(Math.max(1, desiredFactors));
            if (wave <= 2) {
                factorCount = 2 + random.nextInt(2);
            }

            int value = 1;
            for (int i = 0; i < factorCount; i++) {
                List<Integer> choices = new ArrayList<Integer>();
                for (int prime : unlocked) {
                    if (value <= maxValue / prime) {
                        choices.add(prime);
                    }
                }
                if (choices.isEmpty()) {
                    break;
                }
                value *= choices.get(random.nextInt(choices.size()));
            }

            if (value >= 4 && value <= maxValue) {
                if (wave == 1 && value > 18) {
                    continue;
                }
                return value;
            }
        }

        int[] safe = {4, 6, 8, 9, 12, 16, 18};
        return safe[random.nextInt(safe.length)];
    }

    private static int generateSolvablePrimeChallenge(Random random, int maxValue, PlayerInventoryV2 inventory) {
        int maxSubtract = Math.min(12, inventory.getSubtractorCharge());
        if (maxSubtract <= 0 || maxValue < 37) {
            return -1;
        }

        List<Integer> candidates = new ArrayList<Integer>();
        for (int value = 37; value <= maxValue; value++) {
            if (!isPrime(value)) {
                continue;
            }
            if (canPrimeBePreparedBySubtraction(value, maxSubtract, inventory)) {
                candidates.add(value);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean canPrimeBePreparedBySubtraction(int primeValue, int maxSubtract, PlayerInventoryV2 inventory) {
        int[] unlocked = inventory.getUnlockedPrimes();
        for (int subtraction = 1; subtraction <= maxSubtract; subtraction++) {
            int remaining = primeValue - subtraction;
            if (remaining <= 1) {
                continue;
            }
            for (int divisor : unlocked) {
                if (remaining % divisor == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isPrime(int value) {
        if (value < 2) return false;
        if (value == 2) return true;
        if ((value & 1) == 0) return false;
        for (int d = 3; d * d <= value; d += 2) {
            if (value % d == 0) return false;
        }
        return true;
    }

    static float meteorSpeed(int wave, Random random) {
        float base = 37f + Math.min(35f, wave * 2.4f);
        return base * (0.92f + random.nextFloat() * 0.18f);
    }

    static float spawnInterval(int wave) {
        return Math.max(0.82f, 2.35f - (wave - 1) * 0.095f);
    }

    static int meteorsPerWave(int wave) {
        return Math.min(30, 8 + wave * 2);
    }

    static float specialQuizChance(int wave) {
        if (wave < 3) return 0f;
        return Math.min(0.20f, 0.05f + (wave - 3) * 0.012f);
    }

    static float primeChallengeChance(int wave) {
        if (wave < 5) return 0f;
        return Math.min(0.18f, 0.06f + (wave - 5) * 0.012f);
    }

    static float randomBonusChance(float cityHealth, int wave) {
        float healthAssist = Math.max(0f, 100f - cityHealth) / 360f;
        float progression = Math.min(0.05f, wave * 0.004f);
        return Math.min(0.34f, 0.13f + healthAssist + progression);
    }

    static float cityDamage(int value, int wave) {
        float normalized = (float) Math.sqrt(Math.max(2, value) / (float) GLOBAL_MAX_METEOR_VALUE);
        return 4.0f + normalized * 9.0f + Math.min(3.0f, wave * 0.15f);
    }

    static int scoreForDivision(int before, int divisor) {
        return 2 + Math.max(1, divisor / 2) + Math.max(0, before / 40);
    }

    static int scoreForDestroyedMeteor(int originalValue, int wave) {
        return 12 + Math.max(0, originalValue / 5) + Math.max(0, wave - 1) * 2;
    }

    static QuizData createMultiplicationQuiz(Random random, int maxValue) {
        for (int attempt = 0; attempt < 100; attempt++) {
            int a = 2 + random.nextInt(9);
            int b = 2 + random.nextInt(9);
            int result = a * b;
            if (result <= Math.max(20, maxValue)) {
                return new QuizData(a, b, result, true, createAnswers(random, result));
            }
        }
        return new QuizData(4, 4, 16, true, createAnswers(random, 16));
    }

    static QuizData createAdditionQuiz(Random random, int maxValue) {
        int upper = Math.max(12, Math.min(80, maxValue));
        for (int attempt = 0; attempt < 100; attempt++) {
            int a = 2 + random.nextInt(Math.max(2, upper / 2));
            int b = 2 + random.nextInt(Math.max(2, upper / 2));
            int result = a + b;
            if (result <= maxValue && result >= 8) {
                return new QuizData(a, b, result, false, createAnswers(random, result));
            }
        }
        return new QuizData(5, 7, 12, false, createAnswers(random, 12));
    }

    private static int[] createAnswers(Random random, int correct) {
        int[] answers = new int[3];
        int correctSlot = random.nextInt(3);
        for (int i = 0; i < 3; i++) {
            if (i == correctSlot) {
                answers[i] = correct;
                continue;
            }
            int delta;
            do {
                delta = 1 + random.nextInt(Math.max(3, Math.min(12, correct / 3 + 2)));
                if (random.nextBoolean()) delta = -delta;
                answers[i] = Math.max(1, correct + delta);
            } while (answers[i] == correct || (i > 0 && answers[i] == answers[i - 1]));
        }
        return answers;
    }

    static final class QuizData {
        final int a;
        final int b;
        final int result;
        final boolean multiplication;
        final int[] answers;

        QuizData(int a, int b, int result, boolean multiplication, int[] answers) {
            this.a = a;
            this.b = b;
            this.result = result;
            this.multiplication = multiplication;
            this.answers = answers;
        }

        String expression() {
            return a + (multiplication ? " x " : " + ") + b;
        }
    }
}
