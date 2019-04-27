package com.pr0p1k.instaTools.controllers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.pr0p1k.instaTools.InstagramBean
import com.pr0p1k.instaTools.models.Acceptor
import com.pr0p1k.instaTools.models.Donor
import com.pr0p1k.instaTools.models.Source
import com.pr0p1k.instaTools.models.repositories.AcceptorRepository
import com.pr0p1k.instaTools.models.repositories.DonorRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.ZoneOffset

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
        val json = instagram.getJsonNode(login)
        val profileInfo = json["entry_data"]["ProfilePage"][0]["graphql"]["user"]
        val id = profileInfo["id"].asText().toLong()
        val donor = Donor(id, login, !profileInfo["is_private"].asBoolean(), profileInfo.toString())
        donor.lastCheckDate = LocalDateTime.now().minusDays(30) // TODO change that
        donorRepository.save(donor)
    }

    @GetMapping("open_donor")
    fun open(@RequestParam login: String): String {
        return instagram.getRawJson(login)
    }

    @GetMapping("test")
    fun lol(@RequestParam login: String): String {
        val kek = instagram.getJsonNode(login)
        return kek.toString()
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
        val updates = ObjectMapper().createArrayNode()
        val list = acceptorRepository.findById(id).get().listOfDonors
        for (donor in list) {
            if (donor is Donor) {
                val posts = instagram.getPosts(donor.login).filter {
                    LocalDateTime.ofEpochSecond(
                            it["node"]["taken_at_timestamp"].asLong(),
                            0, ZoneOffset.UTC)
                            .isAfter(donor.lastCheckDate)
                }
                updates.addAll(posts)
            }
        }
        return updates.toString()
    }

    //TODO fun for initial caching
}
