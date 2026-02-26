package de.umra.risk.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AnalysisSessionRepository : JpaRepository<AnalysisSessionEntity, UUID>
