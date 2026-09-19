package br.grassinimoraes.divasteroides;

/** Progressao deliberadamente suave: primeiro aprende a regra, depois ganha pressao. */
final class WaveManager {
    static final int MAX_WAVE = 25;

    int wave = 1;
    int destroyedThisWave = 0;
    int targetThisWave = 5;
    int difficulty = 0;

    int maxMeteorValue() {
        int base = 18 + (wave-1)*7 + difficulty*2;
        return Math.min(MeteorMathV2.MAX_METEOR_VALUE, base);
    }

    int currentWeaponValue(){
        if(wave<=1)return 2;
        if(wave==2)return 4;
        if(wave==3)return 8;
        if(wave==4)return 16;
        if(wave==5)return 32;
        if(wave==6)return 64;
        return 128;
    }

    static int weaponAvailableForWave(int phase){
        if(phase<=1)return 2;
        if(phase==2)return 4;
        if(phase==3)return 8;
        if(phase==4)return 16;
        if(phase==5)return 32;
        if(phase==6)return 64;
        return 128;
    }

    int phaseAmmoCapacity(){return Math.max(3,targetThisWave*3);}

    float meteorSpeed() {
        return 24f + wave*1.9f + difficulty*3.2f;
    }

    float spawnSeconds() {
        float s = 2.75f - wave*.052f - difficulty*.08f;
        return Math.max(.82f, s);
    }

    float specialChance() {
        return Math.min(.30f, .13f + (wave-1)*.007f + difficulty*.008f);
    }

    float bonusChance() {
        return Math.min(.22f, .09f + Math.max(0,wave-2)*.004f);
    }

    float commercialPlaneChancePerSecond() {
        if (wave < 3) return 0f;
        return Math.min(.0042f, .0010f + (wave-3)*.00011f);
    }

    void countDestroyed() { destroyedThisWave++; }
    boolean complete() { return destroyedThisWave >= targetThisWave; }
    boolean isFinalWave() { return wave >= MAX_WAVE; }

    void nextWave() {
        if (wave < MAX_WAVE) wave++;
        destroyedThisWave=0;
        targetThisWave = Math.min(12, 4 + wave);
    }
}
