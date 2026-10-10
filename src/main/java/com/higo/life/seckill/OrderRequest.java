package com.higo.life.seckill;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="order_requests",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","voucher_id"}))
public class OrderRequest {
    @Id @Column(length=36) private String id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="voucher_id",nullable=false) private Long voucherId;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false) private boolean sent;
    @Column(nullable=false) private int attempts;
    @Column(length=500) private String error;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    protected OrderRequest() {}
    public OrderRequest(String id,Long userId,Long voucherId) { this.id=id;this.userId=userId;this.voucherId=voucherId;status="ACCEPTED";createdAt=LocalDateTime.now(); }
    public String getId() { return id; } public Long getUserId() { return userId; } public Long getVoucherId() { return voucherId; }
    public String getStatus() { return status; } public boolean isSent() { return sent; } public int getAttempts() { return attempts; } public String getError() { return error; }
    public void dispatched() { sent=true;attempts++;error=null; }
    public void sendFailed(String e) { attempts++;error=e==null?"发送失败":e.substring(0,Math.min(500,e.length())); }
    public void completed() { status="COMPLETED";error=null; }
    public void failed(String e) { status="FAILED";sendFailed(e); }
    public void replay() { status="ACCEPTED";sent=false;error=null; }
}
