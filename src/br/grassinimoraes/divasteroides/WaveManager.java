package br.grassinimoraes.divasteroides;

final class WaveManager {
    int wave = 1;
    int destroyedThisWave = 0;
    int targetThisWave = 10;
    int difficulty = 2;

    int maxMeteorValue() {
        int base = 18 + (wave-1)*9 + difficulty*2;
        return Math.min(200, base);
    }

    int primeUnlockCeiling() {
        int idx = Math.min(MeteorMathV2.PRIMES.length-1, 1 + wave/2 + difficulty/3);
        return MeteorMathV2.PRIMES[idx];
    }

    float meteorSpeed() {
        return 30f + wave*2.6f + difficulty*4.2f;
    }

    float spawnSeconds() {
        float s = 2.35f - wave*.055f - difficulty*.09f;
        return Math.max(.75f, s);
    }

    float specialChance() {
        return Math.min(.28f, .03f + wave*.012f + difficulty*.008f);
    }

    float bonusChance() {
        return Math.min(.25f, .10f + wave*.006f);
    }

    float commercialPlaneChancePerSecond() {
        return Math.min(.0048f, .0014f + wave*.00012f);
    }

    boolean allowLargePrime() { return wave >= 4; }
    void countDestroyed() { destroyedThisWave++; }
    boolean complete() { return destroyedThisWave >= targetThisWave; }

    void nextWave() {
        wave++;
        destroyedThisWave=0;
        targetThisWave = Math.min(22, 9 + wave);
    }
}
