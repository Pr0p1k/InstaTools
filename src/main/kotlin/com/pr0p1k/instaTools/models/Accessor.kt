package com.pr0p1k.instaTools.models

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import kotlin.collections.HashMap

@Entity
data class Accessor(var login: String, var csrfToken: String, var rolloutHash: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0
    lateinit var sessionId: String
    var expirationDate: LocalDateTime? = null

    constructor(login: String, csrfToken: String,
                rolloutHash: String, sessionId: String, expirationDate: LocalDateTime) : this(login, csrfToken, rolloutHash) {
        this.sessionId = sessionId
        this.expirationDate = expirationDate
    }

    companion object {
        val confirmationQueue = HashMap<String, Accessor>()
    }
}
