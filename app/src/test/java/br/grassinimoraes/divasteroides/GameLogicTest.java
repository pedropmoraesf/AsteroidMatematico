package br.grassinimoraes.divasteroides;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class GameLogicTest {
    @Test public void inventarioComecaComDoisETres() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.divisors.contains(2));
        assertTrue(inv.divisors.contains(3));
        assertEquals(3,inv.highestDivisor());
        assertFalse(inv.unlockDivisor(37));
        assertTrue(inv.unlockDivisor(5));
        assertEquals(5,inv.highestDivisor());
    }

    @Test public void subtratorRespeitaCarga() {
        PlayerInventory inv=new PlayerInventory();
        inv.addSubtractor(12);
        assertEquals(12,inv.subtractorCharge);
        assertTrue(inv.spendSubtractor(7));
        assertEquals(5,inv.subtractorCharge);
        assertFalse(inv.spendSubtractor(6));
        assertTrue(inv.spendSubtractor(5));
        assertEquals(0,inv.subtractorCharge);
        assertEquals(1,inv.subtractorValue);
    }

    @Test public void ondasSaoCurtasEProgressivas() {
        WaveManager w=new WaveManager();
        assertEquals(1,w.wave);
        assertEquals(5,w.targetThisWave);
        w.nextWave();
        assertEquals(2,w.wave);
        assertEquals(6,w.targetThisWave);
        for(int i=0;i<40;i++)w.nextWave();
        assertEquals(WaveManager.MAX_WAVE,w.wave);
        assertTrue(w.isFinalWave());
        assertTrue(w.targetThisWave<=12);
        assertTrue(w.maxMeteorValue()<=200);
        assertTrue(w.meteorSpeed()>0f);
        assertTrue(w.spawnSeconds()>=.82f);
    }

    @Test public void meteorosIniciaisSaoResolutiveisComDoisETres() {
        Random r=new Random(12345);
        for(int i=0;i<300;i++){
            int n=MeteorMathV2.generateNormalValue(r,18,3,false);
            assertTrue(n>=2 && n<=18);
            int rest=n;
            while(rest%2==0)rest/=2;
            while(rest%3==0)rest/=3;
            assertEquals("valor nao resolutivel: "+n,1,rest);
        }
    }

    @Test public void valorNormalNuncaUltrapassaDuzentos() {
        Random r=new Random(6789);
        for(int i=0;i<500;i++){
            int n=MeteorMathV2.generateNormalValue(r,999,31,true);
            assertTrue(n<=200);
            assertTrue(n>=2);
        }
    }

    @Test public void quizSempreContemRespostaCorreta() {
        Random r=new Random(2026);
        for(int wave=1;wave<=12;wave++){
            for(int j=0;j<50;j++){
                MeteorMathV2.Quiz q=MeteorMathV2.generateQuiz(r,j%2==0,wave);
                boolean found=false;
                for(int option:q.options)if(option==q.answer)found=true;
                assertTrue(found);
                assertEquals(q.multiplication ? q.a*q.b : q.a+q.b,q.answer);
            }
        }
    }

    @Test public void divisaoSoAceitaDivisorExato() {
        assertTrue(MeteorMathV2.canDivide(18,2));
        assertTrue(MeteorMathV2.canDivide(18,3));
        assertFalse(MeteorMathV2.canDivide(18,5));
        assertFalse(MeteorMathV2.canDivide(0,2));
        assertFalse(MeteorMathV2.canDivide(18,1));
    }
}
