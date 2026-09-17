package br.grassinimoraes.divasteroides;

import java.util.Random;

final class QuizQuestion {
    final int left;
    final int right;
    final int answer;
    final int[] choices;
    final char operation;

    private QuizQuestion(int left, int right, char operation, Random random) {
        this.left = left;
        this.right = right;
        this.operation = operation;
        this.answer = operation == 'x' ? left * right : left + right;
        this.choices = buildChoices(answer, random);
    }

    static QuizQuestion multiplication(Random random, int wave) {
        int max = Math.min(9, 5 + wave / 2);
        int a = 2 + random.nextInt(Math.max(1, max - 1));
        int b = 2 + random.nextInt(Math.max(1, max - 1));
        return new QuizQuestion(a, b, 'x', random);
    }

    static QuizQuestion addition(Random random, int wave) {
        int maxTerm = Math.min(60, 8 + wave * 5);
        int a = 2 + random.nextInt(Math.max(2, maxTerm - 1));
        int b = 2 + random.nextInt(Math.max(2, maxTerm - 1));
        return new QuizQuestion(a, b, '+', random);
    }

    String expression() {
        return left + " " + (operation == 'x' ? "x" : "+") + " " + right;
    }

    private static int[] buildChoices(int answer, Random random) {
        int[] result = new int[3];
        int correctIndex = random.nextInt(3);
        result[correctIndex] = answer;

        for (int i = 0; i < 3; i++) {
            if (i == correctIndex) continue;
            int candidate;
            do {
                int spread = Math.max(3, Math.min(15, answer / 4 + 2));
                candidate = Math.max(0, answer + random.nextInt(spread * 2 + 1) - spread);
            } while (candidate == answer || contains(result, candidate));
            result[i] = candidate;
        }
        return result;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }
}
