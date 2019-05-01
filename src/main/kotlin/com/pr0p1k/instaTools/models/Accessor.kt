package com.pr0p1k.instaTools.models

import org.springframework.data.annotation.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType

@Entity
data class Accessor(var login: String, var sessionId: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0
}
