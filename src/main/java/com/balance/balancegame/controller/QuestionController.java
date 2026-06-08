package com.balance.balancegame.controller;

import com.balance.balancegame.domain.Question;
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

    /** 질문 목록 (결과 바 포함). sort=recent|popular, q=검색어. */
    @GetMapping("/questions")
    public String list(@RequestParam(required = false) String sort,
                       @RequestParam(name = "q", required = false) String keyword,
                       Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : null;
        model.addAttribute("questions", questionService.findAllResults(username, sort, keyword));
        model.addAttribute("sort", sort);
        model.addAttribute("q", keyword);
        return "questions/list";
    }

    /** 마이페이지: 내가 만든 질문 / 내가 투표한 질문. */
    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        model.addAttribute("myQuestions", questionService.findMyQuestions(principal.getName()));
        model.addAttribute("votedQuestions", questionService.findVotedQuestions(principal.getName()));
        return "mypage";
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

    /** 질문 수정 폼 (작성자 본인만). */
    @GetMapping("/questions/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            Question q = questionService.getOwnedQuestion(id, principal.getName());
            QuestionForm form = new QuestionForm();
            form.setTitle(q.getTitle());
            form.setOptionA(q.getOptionA());
            form.setOptionB(q.getOptionB());
            model.addAttribute("questionForm", form);
            model.addAttribute("editId", id);
            return "questions/form";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/questions/" + id;
        }
    }

    /** 질문 수정 처리. */
    @PostMapping("/questions/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute QuestionForm questionForm,
                       BindingResult bindingResult,
                       Principal principal, Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editId", id);
            return "questions/form";
        }
        try {
            questionService.update(id, questionForm, principal.getName());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/questions/" + id;
    }

    /** 질문 삭제 처리 (작성자 본인만). */
    @PostMapping("/questions/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            questionService.delete(id, principal.getName());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/questions/" + id;
        }
        return "redirect:/mypage";
    }
}
