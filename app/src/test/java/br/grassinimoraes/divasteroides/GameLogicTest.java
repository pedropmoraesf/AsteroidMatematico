package br.grassinimoraes.divasteroides;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class GameLogicTest {
    @Test public void inventarioClassicoComecaComDoisETres() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.divisors.contains(2));
        assertTrue(inv.divisors.contains(3));
        assertEquals(3,inv.highestDivisor());
        assertFalse(inv.unlockDivisor(19));
        assertFalse(inv.unlockDivisor(37));
        assertTrue(inv.unlockDivisor(5));
        assertEquals(5,inv.highestDivisor());
    }

    @Test public void arsenalSubtracaoComecaComDoisInfinito() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.hasWeapon(2));
        assertEquals(Integer.MAX_VALUE,inv.ammoForWeapon(2));
        assertTrue(inv.spendWeaponAmmo(2));
        assertEquals(Integer.MAX_VALUE,inv.ammoForWeapon(2));
        assertFalse(inv.hasWeapon(4));
    }

    @Test public void armasFinitasConsomemERecebemMunicao() {
        PlayerInventory inv=new PlayerInventory();
        assertTrue(inv.unlockWeapon(4,18));
        assertEquals(18,inv.ammoForWeapon(4));
        assertTrue(inv.spendWeaponAmmo(4));
        assertEquals(17,inv.ammoForWeapon(4));
        inv.addWeaponAmmo(4,2);
        assertEquals(19,inv.ammoForWeapon(4));
        assertFalse(inv.unlockWeapon(3,10));
    }

    @Test public void armaHConsomeUmaCarga() {
        PlayerInventory inv=new PlayerInventory();
        assertFalse(inv.spendHyper());
        inv.addHyper(2);
        assertTrue(inv.spendHyper());
        assertEquals(1,inv.hyperAmmo);
        assertTrue(inv.spendHyper());
        assertFalse(inv.spendHyper());
    }

    @Test public void reparoCustaDezPorCentoDoSaldo() {
        PlayerInventory inv=new PlayerInventory();
        inv.money=100;
        assertEquals(10,inv.repairCost10Percent());
        assertTrue(inv.repair10Percent(50f));
        assertEquals(90,inv.money);
        assertFalse(inv.repair10Percent(100f));
    }

    @Test public void ondasSaoCurtasEProgressivas() {
        WaveManager w=new WaveManager();
        assertEquals(1,w.wave);
        assertEquals(5,w.targetThisWave);
        assertTrue(w.specialChance()>0f);
        w.nextWave();
        assertEquals(2,w.wave);
        assertEquals(6,w.targetThisWave);
        for(int i=0;i<40;i++)w.nextWave();
        assertEquals(WaveManager.MAX_WAVE,w.wave);
        assertTrue(w.isFinalWave());
        assertTrue(w.targetThisWave<=12);
        assertTrue(w.maxMeteorValue()<=MeteorMathV2.MAX_METEOR_VALUE);
        assertTrue(w.meteorSpeed()>0f);
        assertTrue(w.spawnSeconds()>=.82f);
    }

    @Test public void meteorosClassicosIniciaisSaoResolutiveisComDoisETres() {
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

    @Test public void valoresDaSubtracaoRespeitamLimite() {
        Random r=new Random(6789);
        for(int wave=1;wave<=25;wave++){
            for(int i=0;i<100;i++){
                int n=MeteorMathV2.generateSubtractionValue(r,999,wave);
                assertTrue(n>=2);
                assertTrue(n<=MeteorMathV2.MAX_METEOR_VALUE);
            }
        }
    }

    @Test public void quizTemQuatroOperacoesResultadosNaturaisEContaUnica() {
        Random r=new Random(2026);
        Set<String> used=new HashSet<String>();
        Set<MeteorMathV2.Operation> seen=new HashSet<MeteorMathV2.Operation>();

        for(int difficulty=0;difficulty<=5;difficulty++){
            for(int wave=1;wave<=12;wave++){
                for(int j=0;j<35;j++){
                    MeteorMathV2.Quiz q=MeteorMathV2.generateQuiz(r,difficulty,wave,used);
                    assertNotNull(q);
                    assertTrue(q.answer>=0);
                    assertTrue(used.contains(q.key()));
                    seen.add(q.operation);

                    boolean found=false;
                    for(int option:q.options)if(option==q.answer)found=true;
                    assertTrue(found);

                    switch(q.operation){
                        case ADD:assertEquals(q.a+q.b,q.answer);break;
                        case SUBTRACT:
                            assertTrue(q.a>=q.b);
                            assertEquals(q.a-q.b,q.answer);
                            break;
                        case MULTIPLY:
                            assertTrue(q.b>=2&&q.b<=9);
                            assertEquals(q.a*q.b,q.answer);
                            break;
                        case DIVIDE:
                            assertTrue(q.b>=2&&q.b<=9);
                            assertEquals(0,q.a%q.b);
                            assertEquals(q.a/q.b,q.answer);
                            break;
                    }
                }
            }
        }

        assertEquals(4,seen.size());
    }

    @Test public void divisaoClassicaSoAceitaDivisorExato() {
        assertTrue(MeteorMathV2.canDivide(18,2));
        assertTrue(MeteorMathV2.canDivide(18,3));
        assertFalse(MeteorMathV2.canDivide(18,5));
        assertFalse(MeteorMathV2.canDivide(0,2));
        assertFalse(MeteorMathV2.canDivide(18,1));
    }
}
