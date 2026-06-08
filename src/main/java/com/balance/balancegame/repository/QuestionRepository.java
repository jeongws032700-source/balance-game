package com.balance.balancegame.repository;

import com.balance.balancegame.domain.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Question> rowMapper = (rs, rowNum) -> {
        Question q = new Question();
        q.setId(rs.getLong("id"));
        q.setTitle(rs.getString("title"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setUserId(rs.getLong("user_id"));
        q.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return q;
    };

    public Long save(Question question) {
        String sql = "INSERT INTO questions (title, option_a, option_b, user_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, question.getTitle());
            ps.setString(2, question.getOptionA());
            ps.setString(3, question.getOptionB());
            ps.setLong(4, question.getUserId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * 질문 목록. keyword 가 있으면 제목 LIKE 검색, sort 가 "popular" 면 투표 많은 순, 그 외엔 최신순.
     */
    public List<Question> findAll(String sort, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT q.* FROM questions q");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" WHERE q.title LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if ("popular".equals(sort)) {
            sql.append(" ORDER BY (SELECT COUNT(*) FROM votes v WHERE v.question_id = q.id) DESC, q.id DESC");
        } else {
            sql.append(" ORDER BY q.id DESC");
        }
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public Optional<Question> findById(Long id) {
        String sql = "SELECT * FROM questions WHERE id = ?";
        List<Question> result = jdbcTemplate.query(sql, rowMapper, id);
        return result.stream().findAny();
    }

    /** 특정 사용자가 등록한 질문. */
    public List<Question> findByUserId(Long userId) {
        String sql = "SELECT * FROM questions WHERE user_id = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    /** 특정 사용자가 투표한 질문. */
    public List<Question> findVotedByUserId(Long userId) {
        String sql = "SELECT q.* FROM questions q JOIN votes v ON v.question_id = q.id " +
                "WHERE v.user_id = ? ORDER BY v.id DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public void update(Question question) {
        String sql = "UPDATE questions SET title = ?, option_a = ?, option_b = ? WHERE id = ?";
        jdbcTemplate.update(sql, question.getTitle(), question.getOptionA(),
                question.getOptionB(), question.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id);
    }
}
