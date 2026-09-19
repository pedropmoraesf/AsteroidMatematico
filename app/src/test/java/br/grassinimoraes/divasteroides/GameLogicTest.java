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

    @Test public void ondasTemMetasMaioresEProgressivas() {
        WaveManager w=new WaveManager();
        assertEquals(1,w.wave);
        assertEquals(8,w.targetThisWave);
        assertEquals(8,WaveManager.targetForWave(1));
        assertTrue(w.specialChance()>0f);
        w.nextWave();
        assertEquals(2,w.wave);
        assertEquals(10,w.targetThisWave);
        assertEquals(12,WaveManager.targetForWave(3));
        for(int i=0;i<40;i++)w.nextWave();
        assertEquals(WaveManager.MAX_WAVE,w.wave);
        assertTrue(w.isFinalWave());
        assertEquals(32,w.targetThisWave);
        assertEquals(32,WaveManager.targetForWave(25));
        assertTrue(w.maxMeteorValue()<=MeteorMathV2.MAX_METEOR_VALUE);
        assertTrue(w.meteorSpeed()>0f);
        assertTrue(w.spawnSeconds()>=.82f);
    }

    @Test public void toleranciaDeAcertoDiminuiComADificuldade() {
        assertEquals(1.30f,WaveManager.hitRadiusMultiplier(0),.0001f);
        assertEquals(1.25f,WaveManager.hitRadiusMultiplier(1),.0001f);
        assertEquals(1.20f,WaveManager.hitRadiusMultiplier(2),.0001f);
        assertEquals(1.15f,WaveManager.hitRadiusMultiplier(3),.0001f);
        assertEquals(1.10f,WaveManager.hitRadiusMultiplier(4),.0001f);
        assertEquals(1.00f,WaveManager.hitRadiusMultiplier(5),.0001f);

        // Meteoro de raio 20: ponto a 24 unidades acerta no médio (+20%),
        // mas não no insano (raio exato).
        assertTrue(WaveManager.hitsMeteor(100f,100f,20f,124f,100f,2));
        assertFalse(WaveManager.hitsMeteor(100f,100f,20f,124f,100f,5));

        // O teste é circular: um ponto fora da circunferência ampliada não acerta.
        assertFalse(WaveManager.hitsMeteor(100f,100f,20f,125f,125f,1));
    }

    @Test public void rendaDoMeteoroNuncaPassaDoValorOriginal() {
        assertEquals(80,WaveManager.meteorHitIncome(80,0,80));
        assertEquals(20,WaveManager.meteorHitIncome(80,60,40));
        assertEquals(0,WaveManager.meteorHitIncome(80,80,40));
        assertEquals(0,WaveManager.meteorHitIncome(80,100,40));
        assertEquals(7,WaveManager.meteorHitIncome(80,73,200));
    }

    @Test public void recompensaDoMonumentoCresceEApenalidadeRespeitaDificuldade() {
        assertEquals(79,WaveManager.monumentFullBonus(2));
        assertEquals(91,WaveManager.monumentFullBonus(3));
        assertEquals(21,WaveManager.monumentPenalty(0,2));
        assertEquals(96,WaveManager.monumentPenalty(5,2));
        assertTrue(WaveManager.monumentPenalty(5,10)>WaveManager.monumentPenalty(1,10));
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

    @Test public void x0SimplificaQuizParaOperandosDeUmAlgarismo() {
        Random r=new Random(9090);
        Set<String> used=new HashSet<String>();

        for(MeteorMathV2.Operation op:MeteorMathV2.Operation.values()){
            MeteorMathV2.Quiz source=new MeteorMathV2.Quiz(8,2,op,new int[]{0,1,2});
            MeteorMathV2.Quiz q=MeteorMathV2.simplifyQuizToOneDigit(r,source,used);

            assertEquals(op,q.operation);
            assertTrue(q.a>=0&&q.a<=9);
            assertTrue(q.b>=0&&q.b<=9);
            assertTrue(q.answer>=0);
            assertTrue(used.contains(q.key()));

            boolean found=false;
            for(int option:q.options)if(option==q.answer)found=true;
            assertTrue(found);

            if(op==MeteorMathV2.Operation.SUBTRACT)assertTrue(q.a>=q.b);
            if(op==MeteorMathV2.Operation.MULTIPLY){
                assertTrue(q.a>=1&&q.a<=9);
                assertTrue(q.b>=2&&q.b<=9);
            }
            if(op==MeteorMathV2.Operation.DIVIDE){
                assertTrue(q.a>=1&&q.a<=9);
                assertTrue(q.b>=2&&q.b<=9);
                assertEquals(0,q.a%q.b);
            }
        }
    }

    @Test public void divisaoClassicaSoAceitaDivisorExato() {
        assertTrue(MeteorMathV2.canDivide(18,2));
        assertTrue(MeteorMathV2.canDivide(18,3));
        assertFalse(MeteorMathV2.canDivide(18,5));
        assertFalse(MeteorMathV2.canDivide(0,2));
        assertFalse(MeteorMathV2.canDivide(18,1));
    }
}
