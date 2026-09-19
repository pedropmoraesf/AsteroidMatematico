package br.grassinimoraes.divasteroides;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class GameLogicTest {
    @Test public void arsenalComecaSomenteComDoisInfinito() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.hasWeapon(2));
        assertFalse(inv.hasWeapon(4));
        assertEquals(Integer.MAX_VALUE,inv.ammoFor(2));
        assertTrue(inv.spendAmmo(2));
        assertEquals(Integer.MAX_VALUE,inv.ammoFor(2));
    }

    @Test public void armasFinitasGastamERecebemMunicao() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.unlockWeapon(4,9));
        assertEquals(9,inv.ammoFor(4));
        assertTrue(inv.spendAmmo(4));
        assertEquals(8,inv.ammoFor(4));
        inv.addAmmo(4,2);
        assertEquals(10,inv.ammoFor(4));
        assertFalse(inv.unlockWeapon(3,10));
    }

    @Test public void armaHUsaEstoqueSeparado() {
        PlayerInventory inv=new PlayerInventory();
        assertFalse(inv.spendH());
        inv.addH(2);
        assertTrue(inv.spendH());
        assertEquals(1,inv.hAmmo);
    }

    @Test public void reparoCustaDezPorCentoDoDinheiroERecuperacaoEhControladaPeloJogo() {
        PlayerInventory inv=new PlayerInventory();
        inv.money=101;
        assertEquals(11,inv.repairCost(50f));
        assertTrue(inv.repair(50f));
        assertEquals(90,inv.money);
        assertEquals(0,inv.repairCost(100f));
        assertFalse(inv.repair(100f));
    }

    @Test public void ondasSaoCurtasEProgressivas() {
        WaveManager w=new WaveManager();
        assertEquals(1,w.wave);
        assertEquals(5,w.targetThisWave);
        assertEquals(2,w.currentWeaponValue());
        w.nextWave();
        assertEquals(2,w.wave);
        assertEquals(4,w.currentWeaponValue());
        assertEquals(6,w.targetThisWave);
        for(int i=0;i<40;i++)w.nextWave();
        assertEquals(WaveManager.MAX_WAVE,w.wave);
        assertTrue(w.isFinalWave());
        assertTrue(w.targetThisWave<=12);
        assertTrue(w.maxMeteorValue()<=MeteorMathV2.MAX_METEOR_VALUE);
        assertTrue(w.meteorSpeed()>0f);
        assertTrue(w.spawnSeconds()>=.82f);
    }

    @Test public void progressaoDasSeteArmasEhPotenciaDeDois() {
        int[] esperado={2,4,8,16,32,64,128};
        assertArrayEquals(esperado,MeteorMathV2.WEAPONS);
        for(int fase=1;fase<=7;fase++)assertEquals(esperado[fase-1],WaveManager.weaponAvailableForWave(fase));
        assertEquals(128,WaveManager.weaponAvailableForWave(25));
    }

    @Test public void valorNormalFicaEntreDoisEODoisCentos() {
        Random r=new Random(6789);
        for(int wave=1;wave<=25;wave++){
            for(int i=0;i<100;i++){
                int n=MeteorMathV2.generateNormalValue(r,999,wave,4);
                assertTrue(n<=MeteorMathV2.MAX_METEOR_VALUE);
                assertTrue(n>=2);
            }
        }
    }

    @Test public void quizSempreContemRespostaCorretaENatural() {
        Random r=new Random(2026);
        for(int difficulty=0;difficulty<=4;difficulty++){
            for(int wave=1;wave<=25;wave+=4){
                for(MeteorMathV2.Operation op:MeteorMathV2.Operation.values()){
                    for(int j=0;j<25;j++){
                        MeteorMathV2.Quiz q=MeteorMathV2.generateQuiz(r,op,difficulty,wave);
                        boolean found=false;
                        for(int option:q.options)if(option==q.answer)found=true;
                        assertTrue(found);
                        assertTrue(q.answer>=0);
                        assertEquals(MeteorMathV2.Quiz.solve(q.a,q.b,op),q.answer);
                        if(op==MeteorMathV2.Operation.DIV){
                            assertTrue(q.b>=2&&q.b<=9);
                            assertEquals(0,q.a%q.b);
                        }
                        if(op==MeteorMathV2.Operation.MULT)assertTrue(q.b>=2&&q.b<=9);
                        if(op==MeteorMathV2.Operation.SUB)assertTrue(q.a>=q.b);
                    }
                }
            }
        }
    }

    @Test public void limitesDeDificuldadeSeguemEspecificacao() {
        Random r=new Random(77);
        for(int difficulty=0;difficulty<=4;difficulty++){
            for(int i=0;i<200;i++){
                MeteorMathV2.Quiz mult=MeteorMathV2.generateQuiz(r,MeteorMathV2.Operation.MULT,difficulty,25);
                MeteorMathV2.Quiz div=MeteorMathV2.generateQuiz(r,MeteorMathV2.Operation.DIV,difficulty,25);
                assertTrue(mult.b>=2&&mult.b<=9);
                assertTrue(div.b>=2&&div.b<=9);
                if(difficulty==0){assertTrue(mult.a<=9);assertTrue(div.a<=9);}
                if(difficulty==1){assertTrue(mult.a>=10&&mult.a<=99);assertTrue(div.a>=10&&div.a<=99);}
                if(difficulty==2){assertTrue(mult.a>=100&&mult.a<=999);assertTrue(div.a>=100&&div.a<=999);}
                if(difficulty==3){assertTrue(mult.a>=1000&&mult.a<=9999);assertTrue(div.a>=1000&&div.a<=9999);}
                if(difficulty==4){assertTrue(mult.a>=10000&&mult.a<=99999);assertTrue(div.a>=10000&&div.a<=99999);}
            }
        }
    }
}
