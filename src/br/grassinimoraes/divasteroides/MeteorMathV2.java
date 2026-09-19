package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class MeteorMathV2 {
    // Mantidos para a mecânica clássica de divisores.
    static final int[] PRIMES = {2,3,5,7,11,13,17};

    // Arsenal da nova mecânica: potências sucessivas de 2.
    static final int[] SUBTRACTORS = {2,4,8,16,32,64,128};
    static final int MAX_METEOR_VALUE = 200;

    enum Operation {
        ADD("+"), SUBTRACT("-"), MULTIPLY("x"), DIVIDE("/");
        final String symbol;
        Operation(String symbol){this.symbol=symbol;}
    }

    static final class Quiz {
        final int a, b, answer;
        final Operation operation;
        final int[] options;

        Quiz(int a,int b,Operation operation,int[] options) {
            this.a=a;
            this.b=b;
            this.operation=operation;
            this.answer=answerFor(a,b,operation);
            this.options=options;
        }

        static int answerFor(int a,int b,Operation op) {
            switch(op) {
                case SUBTRACT:return a-b;
                case MULTIPLY:return a*b;
                case DIVIDE:return b==0?0:a/b;
                default:return a+b;
            }
        }

        String expression() { return a+" "+operation.symbol+" "+b; }
        String key() { return operation.name()+":"+a+":"+b; }
    }

    private MeteorMathV2() {}

    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i=2;i*i<=n;i++) if (n%i==0) return false;
        return true;
    }

    static boolean canDivide(int value, int divisor) {
        return divisor > 1 && value > 0 && value % divisor == 0;
    }

    static int generateNormalValue(Random r, int maxValue, int highestUnlockedPrime, boolean allowLargePrime) {
        maxValue = Math.max(6, Math.min(MAX_METEOR_VALUE,maxValue));
        if (allowLargePrime && maxValue >= 37 && r.nextFloat() < 0.13f) {
            List<Integer> primes = new ArrayList<Integer>();
            for (int n=37;n<=maxValue;n++) if (isPrime(n)) primes.add(n);
            if (!primes.isEmpty()) return primes.get(r.nextInt(primes.size()));
        }

        int[] friendly = {4,6,8,9,10,12,14,15,16,18,20,21,24,25,27,28,30,32,35,36,40,42,45,48,49,50,54,56,60,63,64,66,70,72,75,77,80,81,84,88,90,96,98,99,100,108,110,112,120,126,128,132,135,140,144,150,154,156,160,168,175,176,180,189,192,196,198,200};
        List<Integer> candidates = new ArrayList<Integer>();
        for (int n : friendly) {
            if (n > maxValue) break;
            if (fullyFactorableByUnlocked(n, highestUnlockedPrime)) candidates.add(n);
        }
        if (candidates.isEmpty()) return 6;
        return candidates.get(r.nextInt(candidates.size()));
    }

    static int generateSubtractionValue(Random r,int maxValue,int wave) {
        maxValue=Math.max(6,Math.min(MAX_METEOR_VALUE,maxValue));
        int floor=Math.min(maxValue-1,2+Math.max(0,wave-1));
        if (r.nextFloat()<.72f) {
            int steps=Math.max(1,maxValue/2);
            int value=2*(1+r.nextInt(steps));
            if (value>maxValue) value=maxValue;
            return Math.max(2,value);
        }
        return floor+r.nextInt(Math.max(1,maxValue-floor+1));
    }

    private static boolean fullyFactorableByUnlocked(int n, int highest) {
        int remaining=n;
        for (int p : PRIMES) {
            if (p > highest) break;
            while (remaining % p == 0) remaining/=p;
        }
        return remaining==1;
    }

    static Quiz generateQuiz(Random r,int difficulty,int wave,Set<String> used) {
        difficulty=Math.max(0,Math.min(5,difficulty));
        for (int tries=0;tries<120;tries++) {
            Operation op=Operation.values()[r.nextInt(Operation.values().length)];
            Quiz q=buildQuiz(r,difficulty,wave,op);
            if (used==null || used.add(q.key())) return q;
        }

        // O espaço de contas é grande, mas este fallback evita travar uma partida muito longa.
        for (Operation op:Operation.values()) {
            Quiz q=buildQuiz(r,difficulty,wave,op);
            if (used==null || used.add(q.key())) return q;
        }
        return buildQuiz(r,difficulty,wave,Operation.ADD);
    }

    private static Quiz buildQuiz(Random r,int difficulty,int wave,Operation op) {
        int phase=Math.max(1,wave);
        int a,b;

        if (op==Operation.MULTIPLY || op==Operation.DIVIDE) {
            int[] lo={1,1,10,100,1000,10000};
            int[] hi={5,9,99,999,9999,99999};
            int min=lo[difficulty],max=hi[difficulty];
            int phaseMax=scaledMax(min,max,phase);
            int unitMax=Math.min(9,Math.max(difficulty==0?5:6,5+phase/2));
            b=2+r.nextInt(Math.max(1,unitMax-1));

            if (op==Operation.MULTIPLY) {
                a=min+r.nextInt(Math.max(1,phaseMax-min+1));
            } else {
                int qMin=Math.max(1,(min+b-1)/b);
                int qMax=Math.max(qMin,phaseMax/b);
                int q=qMin+r.nextInt(Math.max(1,qMax-qMin+1));
                a=q*b;
            }
        } else {
            int[] maxByDifficulty={20,99,999,9999,99999,999999};
            int max=scaledMax(difficulty==0?10:20,maxByDifficulty[difficulty],phase);
            if (op==Operation.ADD) {
                a=r.nextInt(max+1);
                b=r.nextInt(Math.max(1,max-a+1));
            } else {
                a=r.nextInt(max+1);
                b=r.nextInt(a+1);
            }
        }

        int ans=Quiz.answerFor(a,b,op);
        int[] opts=makeOptions(r,ans);
        return new Quiz(a,b,op,opts);
    }

    private static int scaledMax(int min,int max,int wave) {
        if (max<=min) return max;
        float progress=Math.min(1f,(wave-1)/8f);
        int start=min+Math.max(4,(max-min)/4);
        return Math.min(max,Math.max(min,Math.round(start+(max-start)*progress)));
    }

    private static int[] makeOptions(Random r,int ans) {
        int span=Math.max(3,Math.min(500,Math.max(6,Math.abs(ans)/8)));
        int wrong1=Math.max(0,ans+(r.nextBoolean()?1:-1)*(1+r.nextInt(span)));
        int wrong2=Math.max(0,ans+(r.nextBoolean()?1:-1)*(span+1+r.nextInt(span+2)));
        while (wrong1==ans) wrong1++;
        while (wrong2==ans || wrong2==wrong1) wrong2++;
        int[] opts={ans,wrong1,wrong2};
        for (int i=opts.length-1;i>0;i--) {
            int j=r.nextInt(i+1),t=opts[i];opts[i]=opts[j];opts[j]=t;
        }
        return opts;
    }
}
