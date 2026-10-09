ALTER TABLE shops ADD COLUMN longitude DOUBLE NULL;
ALTER TABLE shops ADD COLUMN latitude DOUBLE NULL;

ALTER TABLE voucher_orders ADD COLUMN request_id VARCHAR(36) NULL;
ALTER TABLE voucher_orders ADD CONSTRAINT uk_order_request UNIQUE (request_id);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    nickname VARCHAR(60) NOT NULL,
    icon VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE follows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_follow_user_target UNIQUE (user_id, target_user_id),
    CONSTRAINT fk_follow_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_follow_target FOREIGN KEY (target_user_id) REFERENCES users (id),
    INDEX idx_follow_target (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE blogs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    liked INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_blog_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_blog_hot (liked DESC, created_at DESC),
    INDEX idx_blog_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
