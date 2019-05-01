package com.pr0p1k.instaTools.models

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Post(val type: String, val url: String, val content: String, val date: Long) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0
}
