package com.pr0p1k.instaTools.models.repositories

import com.pr0p1k.instaTools.models.Donor
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DonorRepository: CrudRepository<Donor, Int> {
    fun findByLogin(login: String): Optional<Donor>
}
