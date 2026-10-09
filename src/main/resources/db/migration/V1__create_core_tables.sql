CREATE TABLE shops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vouchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    begin_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_voucher_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    CONSTRAINT chk_voucher_price CHECK (price >= 0),
    CONSTRAINT chk_voucher_stock CHECK (stock >= 0),
    INDEX idx_voucher_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE voucher_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (id),
    CONSTRAINT uk_order_user_voucher UNIQUE (user_id, voucher_id),
    INDEX idx_order_voucher (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

