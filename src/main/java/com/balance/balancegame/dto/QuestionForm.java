package com.balance.balancegame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionForm {

    @NotBlank(message = "질문 제목을 입력하세요.")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "선택지 A를 입력하세요.")
    @Size(max = 200)
    private String optionA;

    @NotBlank(message = "선택지 B를 입력하세요.")
    @Size(max = 200)
    private String optionB;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }
}
