package com.balance.balancegame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 3, max = 50, message = "아이디는 3~50자입니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 4, max = 50, message = "비밀번호는 4자 이상입니다.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
