package br.grassinimoraes.divasteroides;

import java.util.LinkedHashSet;
import java.util.Set;

final class PlayerInventory {
    final Set<Integer> divisors = new LinkedHashSet<Integer>();
    int selectedDivisor = 2;
    int subtractorCharge = 0;
    int subtractorValue = 1;
    int money = 0;
    int bombZero = 0;
    float shieldSeconds = 0;

    PlayerInventory() {
        divisors.add(2);
        divisors.add(3);
    }

    boolean unlockDivisor(int p) {
        if (p < 2 || p > 31) return false;
        return divisors.add(p);
    }

    int highestDivisor() {
        int m=2;
        for (Integer p : divisors) if (p>m) m=p;
        return m;
    }

    boolean spendSubtractor(int amount) {
        if (amount < 1 || amount > subtractorCharge) return false;
        subtractorCharge -= amount;
        if (subtractorCharge == 0) subtractorValue = 1;
        else if (subtractorValue > subtractorCharge) subtractorValue = subtractorCharge;
        return true;
    }

    void addSubtractor(int amount) {
        subtractorCharge = Math.min(99, subtractorCharge + Math.max(1,amount));
        subtractorValue = Math.min(Math.max(1,subtractorValue),subtractorCharge);
    }

    boolean repair(float cityHealth) {
        if (money < 25 || cityHealth >= 100f) return false;
        money -= 25;
        return true;
    }
}
