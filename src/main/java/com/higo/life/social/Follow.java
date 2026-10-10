package com.higo.life.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(
        name = "uk_follow_user_target", columnNames = {"user_id", "target_user_id"}
))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Follow() {
    }

    public Follow(Long userId, Long targetUserId) {
        this.userId = userId;
        this.targetUserId = targetUserId;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getTargetUserId() { return targetUserId; }
    public Long getUserId() {
        return userId;
    }
}
