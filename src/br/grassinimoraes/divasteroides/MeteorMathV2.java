package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class MeteorMathV2 {
    static final int MAX_METEOR_VALUE = 200;
    static final int[] WEAPONS = {2,4,8,16,32,64,128};
    // Alias mantido para codigo antigo que ainda referencia o nome historico.
    static final int[] PRIMES = WEAPONS;

    enum Operation {
        ADD("+"), SUB("-"), MULT("x"), DIV("/");
        final String symbol;
        Operation(String symbol){this.symbol=symbol;}
    }

    static final class Quiz {
        final int a, b, answer;
        final Operation operation;
        final int[] options;

        Quiz(int a, int b, Operation operation, int[] options) {
            this.a=a;
            this.b=b;
            this.operation=operation;
            this.answer=solve(a,b,operation);
            this.options=options;
        }

        String expression() { return a + " " + operation.symbol + " " + b; }

        static int solve(int a,int b,Operation op){
            switch(op){
                case SUB:return a-b;
                case MULT:return a*b;
                case DIV:return b==0?0:a/b;
                default:return a+b;
            }
        }
    }

    private MeteorMathV2() {}

    static boolean isWeaponValue(int n){
        for(int w:WEAPONS)if(w==n)return true;
        return false;
    }

    static int weaponIndex(int value){
        for(int i=0;i<WEAPONS.length;i++)if(WEAPONS[i]==value)return i;
        return -1;
    }

    static int generateNormalValue(Random r, int maxValue, int wave, int difficulty) {
        maxValue=Math.max(6,Math.min(MAX_METEOR_VALUE,maxValue));
        float progress=Math.min(1f,Math.max(0f,(wave-1)/12f));
        int floor=Math.max(2,Math.round(2+(maxValue*.28f)*progress));
        if(floor>=maxValue)return maxValue;
        int value=floor+r.nextInt(maxValue-floor+1);
        // Mantem bastante variedade, mas privilegia valores pares para combinar
        // visualmente com o arsenal em potencias de 2 sem exigir divisibilidade.
        if(r.nextFloat()<.58f && (value&1)!=0 && value<maxValue)value++;
        return Math.max(2,Math.min(maxValue,value));
    }

    // Compatibilidade com chamadas antigas.
    static int generateNormalValue(Random r, int maxValue, int ignoredHighest, boolean ignoredLargePrime) {
        return generateNormalValue(r,maxValue,1,0);
    }

    private static int digitMinForMulDiv(int difficulty){
        switch(Math.max(0,Math.min(4,difficulty))){
            case 0:return 2;
            case 1:return 10;
            case 2:return 100;
            case 3:return 1000;
            default:return 10000;
        }
    }

    private static int digitMaxForMulDiv(int difficulty){
        switch(Math.max(0,Math.min(4,difficulty))){
            case 0:return 9;
            case 1:return 99;
            case 2:return 999;
            case 3:return 9999;
            default:return 99999;
        }
    }

    private static int maxForAddSub(int difficulty){
        switch(Math.max(0,Math.min(4,difficulty))){
            case 0:return 99;
            case 1:return 999;
            case 2:return 9999;
            case 3:return 99999;
            default:return 999999;
        }
    }

    private static int phaseAdjustedMax(int max,int wave){
        float progress=Math.min(1f,.42f+Math.max(0,wave-1)*.045f);
        return Math.max(9,Math.min(max,Math.round(max*progress)));
    }

    static Quiz generateQuiz(Random r, Operation operation, int difficulty, int wave) {
        int a,b;
        difficulty=Math.max(0,Math.min(4,difficulty));
        if(operation==Operation.MULT){
            int min=digitMinForMulDiv(difficulty);
            int max=phaseAdjustedMax(digitMaxForMulDiv(difficulty),wave);
            if(max<min)max=min;
            a=min+r.nextInt(max-min+1);
            b=2+r.nextInt(8);
        }else if(operation==Operation.DIV){
            int min=digitMinForMulDiv(difficulty);
            int max=phaseAdjustedMax(digitMaxForMulDiv(difficulty),wave);
            if(max<min)max=min;
            b=2+r.nextInt(8);
            int qMin=Math.max(1,(min+b-1)/b);
            int qMax=Math.max(qMin,max/b);
            int q=qMin+r.nextInt(qMax-qMin+1);
            a=q*b;
        }else{
            int max=phaseAdjustedMax(maxForAddSub(difficulty),wave);
            a=r.nextInt(max+1);
            b=r.nextInt(max+1);
            if(operation==Operation.SUB && b>a){int t=a;a=b;b=t;}
        }

        int ans=Quiz.solve(a,b,operation);
        int spread=Math.max(2,Math.min(50000,Math.max(5,Math.abs(ans)/12)));
        List<Integer> wrong=new ArrayList<Integer>();
        int guard=0;
        while(wrong.size()<2 && guard++<80){
            int delta=1+r.nextInt(spread);
            int candidate=ans+(r.nextBoolean()?delta:-delta);
            if(candidate<0||candidate==ans||wrong.contains(candidate))continue;
            wrong.add(candidate);
        }
        while(wrong.size()<2){
            int candidate=ans+wrong.size()+1;
            if(candidate!=ans&&!wrong.contains(candidate))wrong.add(candidate);
        }

        int[] opts={ans,wrong.get(0),wrong.get(1)};
        for(int i=opts.length-1;i>0;i--){
            int j=r.nextInt(i+1);
            int t=opts[i];opts[i]=opts[j];opts[j]=t;
        }
        return new Quiz(a,b,operation,opts);
    }
}
