package de.umra.risk.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Data-access interface for persisting and retrieving analysis sessions.
 */
interface AnalysisSessionRepository : JpaRepository<AnalysisSessionEntity, UUID>
