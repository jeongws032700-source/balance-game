package com.balance.balancegame.dto;

import com.balance.balancegame.domain.Question;

/**
 * 질문 + 투표 집계 결과를 담는 화면용 객체.
 * 바 형식으로 양쪽 퍼센트를 표시하기 위해 percentA / percentB 를 계산해 둔다.
 */
public class QuestionResult {

    private Long id;
    private String title;
    private String optionA;
    private String optionB;
    private long countA;
    private long countB;
    private long total;
    private int percentA;
    private int percentB;
    private String myChoice; // 로그인 사용자가 투표한 선택지("A"/"B"), 안 했으면 null

    public QuestionResult(Question question, long countA, long countB, String myChoice) {
        this.id = question.getId();
        this.title = question.getTitle();
        this.optionA = question.getOptionA();
        this.optionB = question.getOptionB();
        this.countA = countA;
        this.countB = countB;
        this.total = countA + countB;
        this.myChoice = myChoice;
        if (total == 0) {
            this.percentA = 0;
            this.percentB = 0;
        } else {
            this.percentA = (int) Math.round(countA * 100.0 / total);
            this.percentB = 100 - this.percentA;
        }
    }

    public boolean isVoted() {
        return myChoice != null;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public long getCountA() {
        return countA;
    }

    public long getCountB() {
        return countB;
    }

    public long getTotal() {
        return total;
    }

    public int getPercentA() {
        return percentA;
    }

    public int getPercentB() {
        return percentB;
    }

    public String getMyChoice() {
        return myChoice;
    }
}
