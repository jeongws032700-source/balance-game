package com.balance.balancegame.repository;

import com.balance.balancegame.domain.Vote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** 질문 삭제 전, 해당 질문의 투표를 모두 삭제(FK 제약). */
    public void deleteByQuestionId(Long questionId) {
        jdbcTemplate.update("DELETE FROM votes WHERE question_id = ?", questionId);
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

    /**
     * 모든 질문의 선택지별 투표 수를 한 번의 쿼리로 집계.
     * 반환: key=questionId, value=[countA, countB]
     */
    public Map<Long, long[]> countAllGroupedByQuestion() {
        String sql = "SELECT question_id, choice, COUNT(*) AS cnt FROM votes GROUP BY question_id, choice";
        Map<Long, long[]> counts = new HashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> {
            long questionId = rs.getLong("question_id");
            String choice = rs.getString("choice");
            long cnt = rs.getLong("cnt");
            long[] pair = counts.computeIfAbsent(questionId, k -> new long[2]);
            if ("A".equals(choice)) {
                pair[0] = cnt;
            } else if ("B".equals(choice)) {
                pair[1] = cnt;
            }
        });
        return counts;
    }

    /**
     * 특정 사용자의 모든 투표를 한 번의 쿼리로 조회.
     * 반환: key=questionId, value=choice("A"/"B")
     */
    public Map<Long, String> findChoicesByUserId(Long userId) {
        String sql = "SELECT question_id, choice FROM votes WHERE user_id = ?";
        Map<Long, String> choices = new HashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs ->
                choices.put(rs.getLong("question_id"), rs.getString("choice")), userId);
        return choices;
    }
}
