package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.List;

final class PlayerInventoryV2 {
    enum WeaponMode {
        DIVISOR,
        SUBTRACTOR,
        BOMB_ZERO
    }

    private final boolean[] unlockedPrimeSlots = new boolean[GameRulesV2.PRIME_AMMO.length];
    private WeaponMode weaponMode = WeaponMode.DIVISOR;
    private int selectedDivisor = 2;
    private int subtractorCharge;
    private int subtractorValue = 1;
    private int bombZeroCount;
    private int money;
    private int shieldCharges;

    PlayerInventoryV2() {
        reset();
    }

    void reset() {
        for (int i = 0; i < unlockedPrimeSlots.length; i++) {
            unlockedPrimeSlots[i] = false;
        }
        unlockedPrimeSlots[0] = true; // 2
        unlockedPrimeSlots[1] = true; // 3
        weaponMode = WeaponMode.DIVISOR;
        selectedDivisor = 2;
        subtractorCharge = 0;
        subtractorValue = 1;
        bombZeroCount = 0;
        money = 0;
        shieldCharges = 0;
    }

    WeaponMode getWeaponMode() {
        return weaponMode;
    }

    void selectDivisor(int prime) {
        if (isPrimeUnlocked(prime)) {
            selectedDivisor = prime;
            weaponMode = WeaponMode.DIVISOR;
        }
    }

    int getSelectedDivisor() {
        return selectedDivisor;
    }

    boolean isPrimeUnlocked(int prime) {
        int index = GameRulesV2.indexOfPrime(prime);
        return index >= 0 && unlockedPrimeSlots[index];
    }

    boolean unlockPrime(int prime) {
        int index = GameRulesV2.indexOfPrime(prime);
        if (index < 0 || unlockedPrimeSlots[index]) {
            return false;
        }
        unlockedPrimeSlots[index] = true;
        return true;
    }

    int[] getUnlockedPrimes() {
        List<Integer> values = new ArrayList<Integer>();
        for (int i = 0; i < GameRulesV2.PRIME_AMMO.length; i++) {
            if (unlockedPrimeSlots[i]) {
                values.add(GameRulesV2.PRIME_AMMO[i]);
            }
        }
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    void selectSubtractor() {
        if (subtractorCharge > 0) {
            weaponMode = WeaponMode.SUBTRACTOR;
        }
    }

    void addSubtractorCharge(int amount) {
        if (amount <= 0) {
            return;
        }
        subtractorCharge = Math.min(99, subtractorCharge + amount);
        if (subtractorCharge > 0 && subtractorValue > subtractorCharge) {
            subtractorValue = subtractorCharge;
        }
    }

    int getSubtractorCharge() {
        return subtractorCharge;
    }

    int getSubtractorValue() {
        return subtractorValue;
    }

    void adjustSubtractorValue(int delta, int roundMax) {
        int max = Math.max(1, Math.min(roundMax, Math.max(1, subtractorCharge)));
        subtractorValue = Math.max(1, Math.min(max, subtractorValue + delta));
    }

    boolean useSubtractor() {
        if (subtractorCharge <= 0 || subtractorValue <= 0 || subtractorValue > subtractorCharge) {
            return false;
        }
        subtractorCharge -= subtractorValue;
        if (subtractorCharge <= 0) {
            subtractorCharge = 0;
            weaponMode = WeaponMode.DIVISOR;
        }
        if (subtractorValue > Math.max(1, subtractorCharge)) {
            subtractorValue = Math.max(1, subtractorCharge);
        }
        return true;
    }

    void addBombZero(int amount) {
        bombZeroCount = Math.min(9, bombZeroCount + Math.max(0, amount));
    }

    int getBombZeroCount() {
        return bombZeroCount;
    }

    void selectBombZero() {
        if (bombZeroCount > 0) {
            weaponMode = WeaponMode.BOMB_ZERO;
        }
    }

    boolean useBombZero() {
        if (bombZeroCount <= 0) {
            return false;
        }
        bombZeroCount--;
        weaponMode = WeaponMode.DIVISOR;
        return true;
    }

    int getMoney() {
        return money;
    }

    void addMoney(int amount) {
        money = Math.min(999, money + Math.max(0, amount));
    }

    boolean spendMoney(int amount) {
        if (amount <= 0 || money < amount) {
            return false;
        }
        money -= amount;
        return true;
    }

    void addShieldCharge() {
        shieldCharges = Math.min(3, shieldCharges + 1);
    }

    int getShieldCharges() {
        return shieldCharges;
    }

    boolean consumeShieldCharge() {
        if (shieldCharges <= 0) {
            return false;
        }
        shieldCharges--;
        return true;
    }
}
