package com.balance.balancegame.repository;

import com.balance.balancegame.domain.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
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

    public List<Question> findAll() {
        String sql = "SELECT * FROM questions ORDER BY id DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Question> findById(Long id) {
        String sql = "SELECT * FROM questions WHERE id = ?";
        List<Question> result = jdbcTemplate.query(sql, rowMapper, id);
        return result.stream().findAny();
    }
}
