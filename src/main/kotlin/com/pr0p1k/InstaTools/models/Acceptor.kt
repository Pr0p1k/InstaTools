package com.pr0p1k.InstaTools.models

import javax.persistence.*

@Entity
data class Acceptor(var name: String, var url: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0
    @OneToMany
    lateinit var listOfDonors: MutableList<Donor>
}
