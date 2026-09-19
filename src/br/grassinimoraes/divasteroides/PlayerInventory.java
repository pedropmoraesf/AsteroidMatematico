package br.grassinimoraes.divasteroides;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class PlayerInventory {
    final Set<Integer> weapons = new LinkedHashSet<Integer>();
    final Map<Integer,Integer> ammo = new LinkedHashMap<Integer,Integer>();
    int selectedWeapon = 2;
    int hAmmo = 0;
    int money = 0;
    int bombZero = 0;
    float shieldSeconds = 0;

    PlayerInventory() {
        weapons.add(2);
    }

    boolean unlockWeapon(int value,int initialAmmo) {
        if (!MeteorMathV2.isWeaponValue(value)) return false;
        boolean added=weapons.add(value);
        if(value>2){
            int old=ammoFor(value);
            ammo.put(value,Math.max(old,Math.max(0,initialAmmo)));
        }
        return added;
    }

    boolean hasWeapon(int value){return weapons.contains(value);}

    int highestWeapon() {
        int m=2;
        for (Integer value : weapons) if (value>m) m=value;
        return m;
    }

    int ammoFor(int weapon){
        if(weapon==2)return Integer.MAX_VALUE;
        Integer n=ammo.get(weapon);
        return n==null?0:Math.max(0,n);
    }

    void setAmmo(int weapon,int amount){
        if(weapon<=2)return;
        ammo.put(weapon,Math.max(0,amount));
    }

    void addAmmo(int weapon,int amount){
        if(weapon<=2||!MeteorMathV2.isWeaponValue(weapon))return;
        ammo.put(weapon,Math.max(0,ammoFor(weapon)+Math.max(0,amount)));
    }

    boolean spendAmmo(int weapon){
        if(weapon==2)return true;
        if(!hasWeapon(weapon))return false;
        int n=ammoFor(weapon);
        if(n<=0)return false;
        ammo.put(weapon,n-1);
        return true;
    }

    void addH(int amount){hAmmo=Math.max(0,hAmmo+Math.max(0,amount));}
    boolean spendH(){if(hAmmo<=0)return false;hAmmo--;return true;}

    int repairCost(float cityHealth){
        if(cityHealth>=100f||money<=0)return 0;
        return Math.max(1,(int)Math.ceil(money*.10f));
    }

    boolean repair(float cityHealth) {
        int cost=repairCost(cityHealth);
        if(cost<=0||money<cost)return false;
        money-=cost;
        return true;
    }
}
