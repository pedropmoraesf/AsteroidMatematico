package br.grassinimoraes.divasteroides;

/** Progressao deliberadamente suave: primeiro aprende a regra, depois ganha pressao. */
final class WaveManager {
    static final int MAX_WAVE = 25;

    int wave = 1;
    int destroyedThisWave = 0;
    int targetThisWave = targetForWave(1);
    int difficulty = 0;

    int maxMeteorValue() {
        int base = 18 + (wave-1)*7 + difficulty*2;
        return Math.min(MeteorMathV2.MAX_METEOR_VALUE, base);
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
        // As quatro operações do quiz já podem aparecer desde a primeira fase.
        return Math.min(.30f, .11f + (wave-1)*.008f + difficulty*.008f);
    }

    float bonusChance() {
        return Math.min(.24f, .12f + Math.max(0,wave-2)*.0045f);
    }

    float commercialPlaneChancePerSecond() {
        if (wave < 3) return 0f;
        return Math.min(.0042f, .0010f + (wave-3)*.00011f);
    }

    static int targetForWave(int phase) {
        int p=Math.max(1,Math.min(MAX_WAVE,phase));
        // A meta cresce até 32: 8, 10, 12, 14... dando mais duração às fases
        // sem tornar as primeiras cidades excessivamente longas.
        return Math.min(32,8+(p-1)*2);
    }

    boolean allowLargePrime() { return wave >= 5; }
    void countDestroyed() { destroyedThisWave++; }
    boolean complete() { return destroyedThisWave >= targetThisWave; }
    boolean isFinalWave() { return wave >= MAX_WAVE; }

    void nextWave() {
        if (wave < MAX_WAVE) wave++;
        destroyedThisWave=0;
        targetThisWave = targetForWave(wave);
    }
}
