package br.grassinimoraes.divasteroides;

final class WaveManagerV2 {
    private int wave = 1;
    private int spawned;
    private int destroyed;
    private int totalDestroyed;
    private int lost;

    void reset() {
        wave = 1;
        spawned = 0;
        destroyed = 0;
        totalDestroyed = 0;
        lost = 0;
    }

    int getWave() {
        return wave;
    }

    int getMaxMeteorValue() {
        return GameRulesV2.roundMaxValue(wave);
    }

    int getTargetCount() {
        return GameRulesV2.meteorsPerWave(wave);
    }

    float getSpawnInterval() {
        return GameRulesV2.spawnInterval(wave);
    }

    boolean canSpawnMore() {
        return spawned < getTargetCount();
    }

    void markSpawned() {
        spawned++;
    }

    void markDestroyed() {
        destroyed++;
        totalDestroyed++;
    }

    void markLost() {
        lost++;
    }

    int getDestroyedThisWave() {
        return destroyed;
    }

    int getTotalDestroyed() {
        return totalDestroyed;
    }

    int getLostThisWave() {
        return lost;
    }

    boolean allScheduled() {
        return spawned >= getTargetCount();
    }

    void nextWave() {
        wave++;
        spawned = 0;
        destroyed = 0;
        lost = 0;
    }

    float getProgress() {
        int target = Math.max(1, getTargetCount());
        return Math.min(1f, (destroyed + lost) / (float) target);
    }
}
