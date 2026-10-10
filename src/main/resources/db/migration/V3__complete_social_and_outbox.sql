ALTER TABLE users ADD COLUMN bio VARCHAR(500);
ALTER TABLE blogs ADD COLUMN images VARCHAR(2000);
CREATE TABLE blog_likes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    blog_id BIGINT NOT NULL, user_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_blog_like UNIQUE(blog_id,user_id),
    CONSTRAINT fk_like_blog FOREIGN KEY(blog_id) REFERENCES blogs(id),
    CONSTRAINT fk_like_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE blog_comments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    blog_id BIGINT NOT NULL, user_id BIGINT NOT NULL, content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comment_blog FOREIGN KEY(blog_id) REFERENCES blogs(id),
    CONSTRAINT fk_comment_user FOREIGN KEY(user_id) REFERENCES users(id),
    INDEX idx_comment_blog(blog_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE order_requests (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL, voucher_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, sent BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0, error VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_request_buyer UNIQUE(user_id,voucher_id),
    INDEX idx_request_dispatch(sent,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
