package com.pr0p1k.InstaTools.models

import java.time.LocalDateTime
import javax.persistence.*
import kotlin.collections.ArrayList

@Entity
data class Donor(@Id val id: Int, val login: String, var open: Boolean) {
    var accessors: ArrayList<Accessor> = arrayListOf()
    var lastCheckDate: LocalDateTime? = null
}
