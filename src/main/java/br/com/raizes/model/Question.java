package br.com.raizes.model;

public record Question(String prompt, String[] options, int correctOption) { }
