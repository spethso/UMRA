package de.umra.greeting

import org.springframework.data.jpa.repository.JpaRepository

interface GreetingRepository : JpaRepository<GreetingEntity, Long> {
	fun findAllByOrderByIdAsc(): List<GreetingEntity>
}
