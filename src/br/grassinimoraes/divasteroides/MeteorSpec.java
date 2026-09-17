package br.grassinimoraes.divasteroides;

final class MeteorSpec {
    final MeteorType type;
    final int value;
    final int bonusValue;
    final QuizQuestion quiz;

    private MeteorSpec(MeteorType type, int value, int bonusValue, QuizQuestion quiz) {
        this.type = type;
        this.value = value;
        this.bonusValue = bonusValue;
        this.quiz = quiz;
    }

    static MeteorSpec normal(int value) {
        return new MeteorSpec(MeteorType.NORMAL, value, 0, null);
    }

    static MeteorSpec quiz(MeteorType type, QuizQuestion quiz) {
        return new MeteorSpec(type, quiz.answer, 0, quiz);
    }

    static MeteorSpec bonus(MeteorType type, int bonusValue) {
        return new MeteorSpec(type, 0, bonusValue, null);
    }
}
