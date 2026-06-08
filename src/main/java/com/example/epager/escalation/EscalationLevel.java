package com.example.epager.escalation;

import com.example.epager.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "escalation_levels")
public class EscalationLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private EscalationPolicy policy;

    @NotNull
    @Min(1)
    private Integer levelNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private AppUser user;

    @NotNull
    @Min(1)
    private Integer waitMinutes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EscalationPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(EscalationPolicy policy) {
        this.policy = policy;
    }

    public Integer getLevelNumber() {
        return levelNumber;
    }

    public void setLevelNumber(Integer levelNumber) {
        this.levelNumber = levelNumber;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Integer getWaitMinutes() {
        return waitMinutes;
    }

    public void setWaitMinutes(Integer waitMinutes) {
        this.waitMinutes = waitMinutes;
    }
}
