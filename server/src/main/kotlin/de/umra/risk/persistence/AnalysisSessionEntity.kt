package de.umra.risk.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity representing a persisted analysis session.
 *
 * All fields are `val` to enforce immutability after creation.
 *
 * @property id                  unique session identifier
 * @property inputJson           JSON-serialized patient input
 * @property selectedAnalyzerIds JSON array of analyzer ids used
 * @property autoMode            whether analyzers were auto-selected
 * @property resultJson          JSON-serialized analysis response
 * @property createdAt           timestamp when the session was created
 */
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
