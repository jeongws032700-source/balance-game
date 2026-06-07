package com.balance.balancegame.service;

import com.balance.balancegame.domain.User;
import com.balance.balancegame.domain.Vote;
import com.balance.balancegame.repository.UserRepository;
import com.balance.balancegame.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    public VoteService(VoteRepository voteRepository, UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    /** 투표. 한 질문당 한 번만 가능. */
    @Transactional
    public void vote(Long questionId, String choice, String username) {
        if (!"A".equals(choice) && !"B".equals(choice)) {
            throw new IllegalArgumentException("잘못된 선택지입니다.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));

        if (voteRepository.findByQuestionIdAndUserId(questionId, user.getId()).isPresent()) {
            throw new IllegalStateException("이미 투표한 질문입니다.");
        }

        Vote vote = new Vote();
        vote.setQuestionId(questionId);
        vote.setUserId(user.getId());
        vote.setChoice(choice);
        voteRepository.save(vote);
    }
}
