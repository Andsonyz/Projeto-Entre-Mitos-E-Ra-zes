package br.com.raizes.quiz;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public final class QuizBankRegressionTest {
    public static void main(String[] args) {
        int longest=0,shortest=0,total=0;
        check(QuestionBank.QUESTIONS.length==4,"Fases do banco incorretas");
        for(var bank:QuestionBank.QUESTIONS) {
            check(bank.length>=8,"Poucas questões para sortear cinco");
            int[] authorPositions=new int[4];
            for(var question:bank) {
                check(!question.prompt().isBlank()&&question.options().length==4,"Questão incompleta");
                check(new HashSet<>(Arrays.asList(question.options())).size()==4,"Alternativas duplicadas");
                authorPositions[question.correctOption()]++;
                String answer=question.options()[question.correctOption()];
                int length=answer.length(),max=Arrays.stream(question.options()).mapToInt(String::length).max().orElseThrow();
                int min=Arrays.stream(question.options()).mapToInt(String::length).min().orElseThrow();
                if(length==max)longest++;if(length==min)shortest++;total++;
                var observed=new HashSet<Integer>();String[] original=question.options().clone();
                Random random=new Random(0x5EEDC0DEL);
                for(int seed=0;seed<100;seed++) {
                    var shuffled=question.shuffled(random);
                    check(shuffled.options()[shuffled.correctOption()].equals(answer),"Resposta se perdeu ao embaralhar");
                    check(new HashSet<>(Arrays.asList(shuffled.options())).equals(new HashSet<>(Arrays.asList(original))),"Alternativas foram alteradas");
                    observed.add(shuffled.correctOption());
                }
                check(observed.size()==4,"Resposta ficou presa em uma posição");
                check(Arrays.equals(original,question.options()),"Banco original foi modificado");
            }
            for(int frequency:authorPositions)check(frequency==2,"Posições do banco não estão equilibradas");
        }
        check(longest<=total/2 && shortest<=total/2,"Comprimento ainda entrega a resposta");
        System.out.println("OK: "+total+" questões; maior alternativa correta em "+longest+", menor em "+shortest+"; ordens aleatórias preservam respostas.");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
