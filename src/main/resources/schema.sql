-- 밸런스 게임 스키마 (MariaDB)

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questions (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    option_a    VARCHAR(200) NOT NULL,
    option_b    VARCHAR(200) NOT NULL,
    user_id     BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_questions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS votes (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    choice      CHAR(1)     NOT NULL,             -- 'A' 또는 'B'
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votes_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_votes_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT uq_votes_question_user UNIQUE (question_id, user_id)
);
