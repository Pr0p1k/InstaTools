package com.pr0p1k.InstaTools.controllers

import com.fasterxml.jackson.databind.JsonNode
import com.pr0p1k.InstaTools.InstagramBean
import com.pr0p1k.InstaTools.models.Acceptor
import com.pr0p1k.InstaTools.models.Donor
import com.pr0p1k.InstaTools.models.repositories.AcceptorRepository
import com.pr0p1k.InstaTools.models.repositories.DonorRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.StringBuilder

@RestController
class DataController {

    @Autowired
    lateinit var donorRepository: DonorRepository
    @Autowired
    lateinit var instagram: InstagramBean
    @Autowired
    lateinit var acceptorRepository: AcceptorRepository

    @GetMapping("donors")
    fun userList(): Iterable<Donor> {
        return donorRepository.findAll()
    }
    @GetMapping("acceptors")
    fun donorList(): Iterable<Acceptor> {
        return acceptorRepository.findAll()
    }

    @GetMapping("add")
    fun addDonor(@RequestParam login: String) {
        val donor = Donor((Math.random() * 1000).toInt(),login, true)
        donorRepository.save(donor)
    }

    @GetMapping("open_donor")
    fun open(@RequestParam login: String): String {
        return instagram.getRawJson(login)
    }

    @GetMapping("test")
    fun lol(@RequestParam login: String): String {
        val kek = instagram.getJsonNode(login)
        return "kek"
    }

    @GetMapping("add_acceptor")
    fun addAcceptor(@RequestParam name: String, @RequestParam url: String): Int {
        val new = Acceptor(name, url)
        acceptorRepository.save(new)
        return new.id
    }

    @GetMapping("add_in_list")
    fun addDonorInList(@RequestParam id: Int, @RequestParam login: String): ResponseEntity<String> {
        val acceptor = acceptorRepository.findById(id)
        val donor = donorRepository.findByLogin(login)
        if (acceptor.isPresent && donor.isPresent)
            acceptor.get().listOfDonors.add(donor.get())
        else return ResponseEntity.status(404).body("{\"message\":\"Something wasn't found\"}")
        acceptorRepository.save(acceptor.get())
        return ResponseEntity.ok("{\"message\":\"ok\"}")
    }

    @GetMapping("get_updates")
    fun getUpdates(@RequestParam id: Int): String {
        //it should check the date
        val updates = StringBuilder()
        acceptorRepository.findById(id).get().listOfDonors.forEach {
            updates.append(instagram.getRawJson(it.login))
        }
        return updates.toString()
    }

    //TODO fun for initial caching
}
