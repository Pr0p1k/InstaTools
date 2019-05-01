package com.pr0p1k.instaTools.models.repositories

import com.pr0p1k.instaTools.models.Accessor
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AccessorRepository : CrudRepository<Accessor, Int> {
    fun findByLogin(login: String): Optional<Accessor>
}
