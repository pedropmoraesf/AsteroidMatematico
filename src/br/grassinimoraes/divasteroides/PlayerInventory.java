package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.Random;

final class PlayerInventory {
    private final boolean[] unlocked = new boolean[GameRules.PRIME_AMMO.length];
    private int subtractorCharge;
    private int zeroBombs;
    private int nuclearBombs;
    private int money;
    private int destroysSinceAmmoBonus;

    PlayerInventory() { reset(); }

    void reset() {
        for (int i = 0; i < unlocked.length; i++) unlocked[i] = false;
        unlocked[0] = true;
        unlocked[1] = true;
        subtractorCharge = 0;
        zeroBombs = 0;
        nuclearBombs = 0;
        money = 0;
        destroysSinceAmmoBonus = 0;
    }

    boolean isPrimeUnlocked(int prime) {
        for (int i = 0; i < GameRules.PRIME_AMMO.length; i++) {
            if (GameRules.PRIME_AMMO[i] == prime) return unlocked[i];
        }
        return false;
    }

    boolean unlockPrime(int prime) {
        for (int i = 0; i < GameRules.PRIME_AMMO.length; i++) {
            if (GameRules.PRIME_AMMO[i] == prime) {
                boolean changed = !unlocked[i];
                unlocked[i] = true;
                return changed;
            }
        }
        return false;
    }

    ArrayList<Integer> getUnlockedPrimes() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < unlocked.length; i++) if (unlocked[i]) result.add(GameRules.PRIME_AMMO[i]);
        return result;
    }

    int randomLockedPrimeEligible(Random random, int wave) {
        int maxIndex = GameRules.highestBonusPrimeIndexForWave(wave);
        ArrayList<Integer> locked = new ArrayList<Integer>();
        for (int i = 2; i <= maxIndex; i++) if (!unlocked[i]) locked.add(GameRules.PRIME_AMMO[i]);
        if (locked.isEmpty()) return 0;
        return locked.get(random.nextInt(locked.size()));
    }

    void noteMeteorDestroyed() { destroysSinceAmmoBonus++; }
    int getDestroysSinceAmmoBonus() { return destroysSinceAmmoBonus; }
    void resetDestroysSinceAmmoBonus() { destroysSinceAmmoBonus = 0; }

    int getSubtractorCharge() { return subtractorCharge; }
    void addSubtractorCharge(int amount) { subtractorCharge = Math.min(99, subtractorCharge + Math.max(0, amount)); }
    boolean canSpendSubtractor(int amount) { return amount > 0 && amount <= subtractorCharge; }
    boolean spendSubtractor(int amount) {
        if (!canSpendSubtractor(amount)) return false;
        subtractorCharge -= amount;
        return true;
    }

    int getZeroBombs() { return zeroBombs; }
    void addZeroBomb() { zeroBombs = Math.min(9, zeroBombs + 1); }
    boolean useZeroBomb() { if (zeroBombs <= 0) return false; zeroBombs--; return true; }

    int getNuclearBombs() { return nuclearBombs; }
    void addNuclearBomb() { nuclearBombs = Math.min(3, nuclearBombs + 1); }
    boolean useNuclearBomb() { if (nuclearBombs <= 0) return false; nuclearBombs--; return true; }

    int getMoney() { return money; }
    void addMoney(int amount) { money = Math.min(999, money + Math.max(0, amount)); }
    boolean spendMoney(int amount) { if (money < amount) return false; money -= amount; return true; }
}
