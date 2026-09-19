package br.grassinimoraes.divasteroides;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class PlayerInventory {
    // Arsenal clássico de divisores.
    final Set<Integer> divisors = new LinkedHashSet<Integer>();
    int selectedDivisor = 2;

    // Arsenal da nova mecânica de subtração.
    final Set<Integer> weapons = new LinkedHashSet<Integer>();
    final Map<Integer,Integer> weaponAmmo = new LinkedHashMap<Integer,Integer>();
    int selectedWeapon = 2;
    int hyperAmmo = 0;

    int money = 0;
    int bombZero = 0;
    float shieldSeconds = 0;

    PlayerInventory() {
        resetArsenal();
    }

    void resetArsenal() {
        divisors.clear();
        divisors.add(2);
        divisors.add(3);
        selectedDivisor=2;

        weapons.clear();
        weaponAmmo.clear();
        weapons.add(2);
        selectedWeapon=2;
        hyperAmmo=0;
    }

    boolean unlockDivisor(int p) {
        if (p < 2 || p > 17 || !MeteorMathV2.isPrime(p)) return false;
        return divisors.add(p);
    }

    int highestDivisor() {
        int m=2;
        for (Integer p : divisors) if (p>m) m=p;
        return m;
    }

    boolean isWeaponValue(int value) {
        for (int v : MeteorMathV2.SUBTRACTORS) if (v==value) return true;
        return false;
    }

    boolean hasWeapon(int value) {
        return weapons.contains(value);
    }

    boolean unlockWeapon(int value, int initialAmmo) {
        if (!isWeaponValue(value)) return false;
        boolean added=weapons.add(value);
        if (value!=2 && initialAmmo>0) addWeaponAmmo(value,initialAmmo);
        if (added && selectedWeapon==2) selectedWeapon=value;
        return added;
    }

    int ammoForWeapon(int value) {
        if (value==2) return Integer.MAX_VALUE;
        Integer n=weaponAmmo.get(value);
        return n==null?0:Math.max(0,n);
    }

    boolean spendWeaponAmmo(int value) {
        if (!hasWeapon(value)) return false;
        if (value==2) return true;
        int n=ammoForWeapon(value);
        if (n<=0) return false;
        weaponAmmo.put(value,n-1);
        return true;
    }

    void addWeaponAmmo(int value,int amount) {
        if (!isWeaponValue(value) || value==2 || amount<=0) return;
        weaponAmmo.put(value,Math.min(999,ammoForWeapon(value)+amount));
    }

    int highestOwnedWeapon() {
        int best=2;
        for (Integer w:weapons) if (w>best) best=w;
        return best;
    }

    int preferredFiniteWeapon() {
        if (selectedWeapon>2 && hasWeapon(selectedWeapon)) return selectedWeapon;
        int best=0;
        for (Integer w:weapons) if (w>2 && w>best) best=w;
        return best;
    }

    boolean spendHyper() {
        if (hyperAmmo<=0) return false;
        hyperAmmo--;
        return true;
    }

    void addHyper(int amount) {
        if (amount>0) hyperAmmo=Math.min(99,hyperAmmo+amount);
    }

    int repairCost10Percent() {
        if (money<=0) return 0;
        return Math.max(1,(int)Math.ceil(money*.10));
    }

    boolean repair10Percent(float cityHealth) {
        if (cityHealth>=100f || money<=0) return false;
        int cost=repairCost10Percent();
        if (cost<=0 || cost>money) return false;
        money-=cost;
        return true;
    }

    String serializeWeapons() {
        StringBuilder s=new StringBuilder();
        for (Integer w:weapons) {
            if (s.length()>0) s.append(',');
            s.append(w);
        }
        return s.toString();
    }

    void restoreWeapons(String raw) {
        weapons.clear();
        weapons.add(2);
        if (raw!=null) {
            for (String part:raw.split(",")) {
                try {
                    int w=Integer.parseInt(part.trim());
                    if (isWeaponValue(w)) weapons.add(w);
                } catch(Exception ignored) {}
            }
        }
        if (!weapons.contains(selectedWeapon)) selectedWeapon=2;
    }

    String serializeWeaponAmmo() {
        StringBuilder s=new StringBuilder();
        for (int w:MeteorMathV2.SUBTRACTORS) {
            if (w==2) continue;
            int n=ammoForWeapon(w);
            if (n<=0) continue;
            if (s.length()>0) s.append(',');
            s.append(w).append(':').append(n);
        }
        return s.toString();
    }

    void restoreWeaponAmmo(String raw) {
        weaponAmmo.clear();
        if (raw==null) return;
        for (String token:raw.split(",")) {
            try {
                String[] p=token.split(":");
                if (p.length!=2) continue;
                int w=Integer.parseInt(p[0].trim());
                int n=Integer.parseInt(p[1].trim());
                if (isWeaponValue(w) && w!=2 && n>0) weaponAmmo.put(w,Math.min(999,n));
            } catch(Exception ignored) {}
        }
    }
}
