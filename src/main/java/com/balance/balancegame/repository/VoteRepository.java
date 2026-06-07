package com.balance.balancegame.repository;

import com.balance.balancegame.domain.Vote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VoteRepository {

    private final JdbcTemplate jdbcTemplate;

    public VoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Vote> rowMapper = (rs, rowNum) -> {
        Vote v = new Vote();
        v.setId(rs.getLong("id"));
        v.setQuestionId(rs.getLong("question_id"));
        v.setUserId(rs.getLong("user_id"));
        v.setChoice(rs.getString("choice"));
        v.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return v;
    };

    public void save(Vote vote) {
        String sql = "INSERT INTO votes (question_id, user_id, choice) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, vote.getQuestionId(), vote.getUserId(), vote.getChoice());
    }

    /** 특정 질문에서 선택지별 투표 수를 센다. */
    public long countByQuestionIdAndChoice(Long questionId, String choice) {
        String sql = "SELECT COUNT(*) FROM votes WHERE question_id = ? AND choice = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, questionId, choice);
        return count == null ? 0 : count;
    }

    /** 로그인 사용자가 해당 질문에 투표했는지 조회. */
    public Optional<Vote> findByQuestionIdAndUserId(Long questionId, Long userId) {
        String sql = "SELECT * FROM votes WHERE question_id = ? AND user_id = ?";
        List<Vote> result = jdbcTemplate.query(sql, rowMapper, questionId, userId);
        return result.stream().findAny();
    }
}
