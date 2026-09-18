package br.grassinimoraes.divasteroides;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class MeteorMathV2 {
    static final int[] PRIMES = {2,3,5,7,11,13,17};

    static final class Quiz {
        final int a, b, answer;
        final boolean multiplication;
        final int[] options;
        Quiz(int a, int b, boolean multiplication, int[] options) {
            this.a=a; this.b=b; this.multiplication=multiplication;
            this.answer = multiplication ? a*b : a+b;
            this.options=options;
        }
        String expression() { return a + (multiplication ? " x " : " + ") + b; }
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
        maxValue = Math.max(6, Math.min(200,maxValue));
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

    private static boolean fullyFactorableByUnlocked(int n, int highest) {
        int remaining=n;
        for (int p : PRIMES) {
            if (p > highest) break;
            while (remaining % p == 0) remaining/=p;
        }
        return remaining==1;
    }

    static Quiz generateQuiz(Random r, boolean multiplication, int wave) {
        int a, b;
        if (multiplication) {
            int max = Math.min(10, 4 + wave/2);
            a = 2 + r.nextInt(Math.max(1, max-1));
            b = 2 + r.nextInt(Math.max(1, max-1));
        } else {
            int max = Math.min(50, 12 + wave*3);
            a = 2 + r.nextInt(Math.max(2, max/2));
            b = 2 + r.nextInt(Math.max(2, max/2));
        }
        int ans = multiplication ? a*b : a+b;
        int wrong1 = Math.max(1, ans + (r.nextBoolean()?1:-1)*(1+r.nextInt(5)));
        int wrong2 = Math.max(1, ans + (r.nextBoolean()?1:-1)*(6+r.nextInt(7)));
        while (wrong2 == wrong1 || wrong2 == ans) wrong2++;
        int[] opts = {ans, wrong1, wrong2};
        for (int i=opts.length-1;i>0;i--) {
            int j=r.nextInt(i+1); int t=opts[i]; opts[i]=opts[j]; opts[j]=t;
        }
        return new Quiz(a,b,multiplication,opts);
    }
}
