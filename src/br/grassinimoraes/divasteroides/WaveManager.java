package br.grassinimoraes.divasteroides;

import java.util.Random;

final class WaveManager {
    private int wave = 1;
    private int resolvedInWave;
    private boolean warningActive;
    private final Random random = new Random();

    void reset() {
        wave = 1;
        resolvedInWave = 0;
        warningActive = false;
    }

    int getWave() { return wave; }
    int getMaxMeteorValue() { return GameRules.maxMeteorValueForWave(wave); }
    float getSpawnInterval() { return GameRules.spawnInterval(wave); }
    boolean isWarningActive() { return warningActive; }
    void setWarningActive(boolean active) { warningActive = active; }

    boolean onHostileMeteorResolved() {
        if (warningActive) return false;
        resolvedInWave++;
        if (resolvedInWave >= GameRules.METEORS_PER_WAVE) {
            resolvedInWave = 0;
            wave++;
            return true;
        }
        return false;
    }

    MeteorSpec nextMeteor(PlayerInventory inventory, float cityHealth) {
        if (random.nextFloat() < GameRules.bonusSpawnChance(wave, cityHealth)) {
            return GameRules.randomBonus(random, wave, inventory, cityHealth);
        }

        if (wave >= 2 && random.nextFloat() < GameRules.quizChance(wave)) {
            if (random.nextBoolean()) {
                return MeteorSpec.quiz(MeteorType.ADDITION, QuizQuestion.addition(random, wave));
            }
            return MeteorSpec.quiz(MeteorType.MULTIPLICATION, QuizQuestion.multiplication(random, wave));
        }

        if (wave >= 4 && inventory.getSubtractorCharge() > 0 && random.nextInt(100) < Math.min(24, 5 + wave * 2)) {
            return MeteorSpec.normal(GameRules.generatePrimeChallenge(random, wave));
        }

        return MeteorSpec.normal(GameRules.generateComposite(random, wave, inventory));
    }

    MeteorSpec guaranteedBonus(PlayerInventory inventory, float cityHealth) {
        return GameRules.randomBonus(random, wave, inventory, cityHealth);
    }
}
