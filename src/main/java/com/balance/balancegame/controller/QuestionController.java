package com.balance.balancegame.controller;

import com.balance.balancegame.dto.QuestionForm;
import com.balance.balancegame.service.QuestionService;
import com.balance.balancegame.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final VoteService voteService;

    public QuestionController(QuestionService questionService, VoteService voteService) {
        this.questionService = questionService;
        this.voteService = voteService;
    }

    /** 루트는 질문 목록으로. */
    @GetMapping("/")
    public String index() {
        return "redirect:/questions";
    }

    /** 질문 목록 (결과 바 포함). */
    @GetMapping("/questions")
    public String list(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : null;
        model.addAttribute("questions", questionService.findAllResults(username));
        return "questions/list";
    }

    /** 질문 등록 폼. */
    @GetMapping("/questions/new")
    public String newForm(Model model) {
        model.addAttribute("questionForm", new QuestionForm());
        return "questions/form";
    }

    /** 질문 등록 처리. */
    @PostMapping("/questions")
    public String create(@Valid @ModelAttribute QuestionForm questionForm,
                         BindingResult bindingResult,
                         Principal principal) {
        if (bindingResult.hasErrors()) {
            return "questions/form";
        }
        Long id = questionService.create(questionForm, principal.getName());
        return "redirect:/questions/" + id;
    }

    /** 질문 상세 + 투표 결과. */
    @GetMapping("/questions/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : null;
        model.addAttribute("result", questionService.findResult(id, username));
        model.addAttribute("loggedIn", principal != null);
        return "questions/detail";
    }

    /** 투표 처리. */
    @PostMapping("/questions/{id}/vote")
    public String vote(@PathVariable Long id,
                       @RequestParam String choice,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        try {
            voteService.vote(id, choice, principal.getName());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/questions/" + id;
    }
}
