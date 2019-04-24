package com.pr0p1k.instaTools.models

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import javax.persistence.*
import kotlin.collections.ArrayList

@Entity
data class Donor(val donorId: Long, val login: String, var open: Boolean, var profileInfo: String): Source() {
    var accessors: ArrayList<Accessor> = arrayListOf()
    var lastCheckDate: LocalDateTime? = null
    @OneToMany
    val posts = mutableListOf<Post>()
}
