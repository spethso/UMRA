package de.umra.risk.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "analysis_sessions")
class AnalysisSessionEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "TEXT", nullable = false)
    val inputJson: String = "",

    @Column(columnDefinition = "TEXT")
    val selectedAnalyzerIds: String = "",

    @Column(nullable = false)
    val autoMode: Boolean = false,

    @Column(columnDefinition = "TEXT")
    val resultJson: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)
