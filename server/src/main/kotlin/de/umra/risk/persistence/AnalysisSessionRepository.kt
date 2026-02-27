package de.umra.risk.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [AnalysisSessionEntity].
 *
 * Standard CRUD operations are inherited from [JpaRepository].
 */
interface AnalysisSessionRepository : JpaRepository<AnalysisSessionEntity, UUID>
