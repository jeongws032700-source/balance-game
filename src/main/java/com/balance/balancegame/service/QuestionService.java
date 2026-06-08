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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    /**
     * 질문 목록을 결과(집계) 형태로 반환. sort/keyword 로 정렬·검색.
     * username 이 null 이면 내 투표 표시 없음.
     * 질문 수와 무관하게 쿼리는 최대 3번(질문 목록 + 투표 집계 + 내 투표)만 발생한다.
     */
    @Transactional(readOnly = true)
    public List<QuestionResult> findAllResults(String username, String sort, String keyword) {
        Long userId = resolveUserId(username);
        return assemble(questionRepository.findAll(sort, keyword), userId);
    }

    /** 내가 등록한 질문 목록(집계 포함). */
    @Transactional(readOnly = true)
    public List<QuestionResult> findMyQuestions(String username) {
        Long userId = resolveUserId(username);
        if (userId == null) {
            return List.of();
        }
        return assemble(questionRepository.findByUserId(userId), userId);
    }

    /** 내가 투표한 질문 목록(집계 포함). */
    @Transactional(readOnly = true)
    public List<QuestionResult> findVotedQuestions(String username) {
        Long userId = resolveUserId(username);
        if (userId == null) {
            return List.of();
        }
        return assemble(questionRepository.findVotedByUserId(userId), userId);
    }

    /** 질문 목록 + 투표 집계 + 내 투표를 모아 결과 DTO 리스트로 만든다. (집계/내투표는 각각 1쿼리) */
    private List<QuestionResult> assemble(List<Question> questions, Long userId) {
        Map<Long, long[]> countMap = voteRepository.countAllGroupedByQuestion();
        Map<Long, String> myChoices = (userId != null)
                ? voteRepository.findChoicesByUserId(userId)
                : Collections.emptyMap();

        List<QuestionResult> results = new ArrayList<>();
        for (Question q : questions) {
            long[] counts = countMap.getOrDefault(q.getId(), new long[2]);
            results.add(new QuestionResult(q, counts[0], counts[1], myChoices.get(q.getId())));
        }
        return results;
    }

    /** 수정/삭제 전 작성자 본인 여부 확인 후 질문을 반환. */
    @Transactional(readOnly = true)
    public Question getOwnedQuestion(Long id, String username) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
        if (!q.getUserId().equals(user.getId())) {
            throw new IllegalStateException("본인이 등록한 질문만 수정/삭제할 수 있습니다.");
        }
        return q;
    }

    /** 질문 수정 (작성자 본인만). */
    @Transactional
    public void update(Long id, QuestionForm form, String username) {
        Question q = getOwnedQuestion(id, username);
        q.setTitle(form.getTitle());
        q.setOptionA(form.getOptionA());
        q.setOptionB(form.getOptionB());
        questionRepository.update(q);
    }

    /** 질문 삭제 (작성자 본인만). 투표 기록도 함께 삭제. */
    @Transactional
    public void delete(Long id, String username) {
        Question q = getOwnedQuestion(id, username);
        voteRepository.deleteByQuestionId(q.getId());
        questionRepository.delete(q.getId());
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
