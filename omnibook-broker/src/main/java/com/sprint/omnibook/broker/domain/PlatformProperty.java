package com.sprint.omnibook.broker.domain;

import com.sprint.omnibook.broker.event.PlatformType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OTA 플랫폼 숙소 매핑 엔티티.
 * 플랫폼별 숙소 ID를 내부 Property에 매핑.
 */
@Entity
@Table(name = "platform_property", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform_type", "platform_property_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    private PlatformType platformType;

    @Column(name = "platform_property_id", nullable = false)
    private String platformPropertyId;

    /** 플랫폼에서 제공한 원본 숙소명 (디버깅/매칭 로그용) */
    @Column(name = "platform_property_name")
    private String platformPropertyName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public PlatformProperty(Property property, PlatformType platformType,
                            String platformPropertyId, String platformPropertyName) {
        this.property = property;
        this.platformType = platformType;
        this.platformPropertyId = platformPropertyId;
        this.platformPropertyName = platformPropertyName;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
