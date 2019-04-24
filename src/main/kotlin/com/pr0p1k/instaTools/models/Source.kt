package com.pr0p1k.instaTools.models

import javax.persistence.*

@MappedSuperclass
open class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0
}
