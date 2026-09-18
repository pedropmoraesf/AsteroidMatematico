package br.grassinimoraes.divasteroides;

/** Progressao deliberadamente suave: primeiro aprende a regra, depois ganha pressao. */
final class WaveManager {
    int wave = 1;
    int destroyedThisWave = 0;
    int targetThisWave = 5;
    int difficulty = 0;

    int maxMeteorValue() {
        int base = 18 + (wave-1)*7 + difficulty*2;
        return Math.min(200, base);
    }

    int primeUnlockCeiling() {
        int idx = Math.min(MeteorMathV2.PRIMES.length-1, 1 + Math.max(0,(wave-1)/2) + difficulty/3);
        return MeteorMathV2.PRIMES[idx];
    }

    float meteorSpeed() {
        return 24f + wave*1.9f + difficulty*3.2f;
    }

    float spawnSeconds() {
        float s = 2.75f - wave*.052f - difficulty*.08f;
        return Math.max(.82f, s);
    }

    float specialChance() {
        if (wave < 3) return 0f;
        return Math.min(.25f, .035f + (wave-3)*.011f + difficulty*.006f);
    }

    float bonusChance() {
        return Math.min(.24f, .12f + Math.max(0,wave-2)*.0045f);
    }

    float commercialPlaneChancePerSecond() {
        if (wave < 3) return 0f;
        return Math.min(.0042f, .0010f + (wave-3)*.00011f);
    }

    boolean allowLargePrime() { return wave >= 5; }
    void countDestroyed() { destroyedThisWave++; }
    boolean complete() { return destroyedThisWave >= targetThisWave; }

    void nextWave() {
        wave++;
        destroyedThisWave=0;
        targetThisWave = Math.min(12, 4 + wave);
    }
}
