package com.balance.balancegame.service;

import com.balance.balancegame.domain.Question;
import com.balance.balancegame.domain.User;
import com.balance.balancegame.domain.Vote;
import com.balance.balancegame.dto.QuestionForm;
import com.balance.balancegame.dto.QuestionResult;
import com.balance.balancegame.repository.QuestionRepository;
import com.balance.balancegame.repository.UserRepository;
import com.balance.balancegame.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    public QuestionService(QuestionRepository questionRepository,
                           VoteRepository voteRepository,
                           UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    /** 질문 등록 (로그인 사용자 누구나). */
    @Transactional
    public Long create(QuestionForm form, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
        Question question = new Question();
        question.setTitle(form.getTitle());
        question.setOptionA(form.getOptionA());
        question.setOptionB(form.getOptionB());
        question.setUserId(user.getId());
        return questionRepository.save(question);
    }

    /** 전체 질문을 결과(집계) 형태로 반환. username 이 null 이면 내 투표 표시 없음. */
    @Transactional(readOnly = true)
    public List<QuestionResult> findAllResults(String username) {
        Long userId = resolveUserId(username);
        List<QuestionResult> results = new ArrayList<>();
        for (Question q : questionRepository.findAll()) {
            results.add(toResult(q, userId));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public QuestionResult findResult(Long questionId, String username) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        return toResult(q, resolveUserId(username));
    }

    private QuestionResult toResult(Question q, Long userId) {
        long countA = voteRepository.countByQuestionIdAndChoice(q.getId(), "A");
        long countB = voteRepository.countByQuestionIdAndChoice(q.getId(), "B");
        String myChoice = null;
        if (userId != null) {
            Optional<Vote> myVote = voteRepository.findByQuestionIdAndUserId(q.getId(), userId);
            myChoice = myVote.map(Vote::getChoice).orElse(null);
        }
        return new QuestionResult(q, countA, countB, myChoice);
    }

    private Long resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }
}
