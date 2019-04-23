package com.pr0p1k.InstaTools.models.repositories

import com.pr0p1k.InstaTools.models.Donor
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DonorRepository: CrudRepository<Donor, Int> {
    fun findByLogin(login: String): Optional<Donor>
}
