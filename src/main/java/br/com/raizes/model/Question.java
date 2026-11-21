package br.com.raizes.model;

public record Question(String prompt, String[] options, int correctOption) {
    public Question shuffled(java.util.Random random) {
        java.util.List<Integer> order = new java.util.ArrayList<>(java.util.List.of(0, 1, 2, 3));
        java.util.Collections.shuffle(order, random);
        String[] result = new String[4];
        for (int index = 0; index < result.length; index++) result[index] = options[order.get(index)];
        return new Question(prompt, result, order.indexOf(correctOption));
    }
}
