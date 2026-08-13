package com.skillmatrix.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}
