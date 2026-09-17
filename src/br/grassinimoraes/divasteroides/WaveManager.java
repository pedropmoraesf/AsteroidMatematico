package br.grassinimoraes.divasteroides;

/**
 * Progressao deliberadamente gradual: a dificuldade muda principalmente a
 * pressao (velocidade e intervalo), enquanto a complexidade matematica sobe
 * com as ondas. As novas municoes nunca sao dadas automaticamente: este
 * gerenciador apenas libera o teto para que elas possam aparecer como bonus.
 */
final class WaveManager {
    int wave = 1;
    int destroyedThisWave = 0;
    int targetThisWave = 8;
    int difficulty = 1;

    int maxMeteorValue() {
        int pressureBonus = Math.max(0, difficulty - 1) * 3;
        int base = 18 + (wave - 1) * 8 + pressureBonus;
        return Math.min(200, base);
    }

    int primeUnlockCeiling() {
        int index = 1 + (wave - 1) / 2;
        if (difficulty >= 3 && wave >= 4) index++;
        index = Math.min(MeteorMathV2.PRIMES.length - 1, index);
        return MeteorMathV2.PRIMES[index];
    }

    float meteorSpeed() {
        return 25f + wave * 1.85f + difficulty * 3.1f;
    }

    float spawnSeconds() {
        float seconds = 2.75f - wave * .045f - difficulty * .10f;
        return Math.max(.92f, seconds);
    }

    float specialChance() {
        if (wave < 3) return 0f;
        return Math.min(.24f, .035f + (wave - 3) * .011f + difficulty * .006f);
    }

    float bonusChance() {
        return Math.min(.24f, .11f + wave * .005f);
    }

    float commercialPlaneChancePerSecond() {
        if (wave < 2) return 0f;
        return Math.min(.0018f, .00042f + wave * .000055f);
    }

    boolean allowLargePrime() {
        return wave >= 6;
    }

    void countDestroyed() {
        destroyedThisWave++;
    }

    boolean complete() {
        return destroyedThisWave >= targetThisWave;
    }

    void nextWave() {
        wave++;
        destroyedThisWave = 0;
        targetThisWave = Math.min(22, 7 + wave);
    }
}
