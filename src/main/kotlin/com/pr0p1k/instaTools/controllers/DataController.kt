package com.pr0p1k.instaTools.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.pr0p1k.instaTools.InstagramBean
import com.pr0p1k.instaTools.models.Acceptor
import com.pr0p1k.instaTools.models.Donor
import com.pr0p1k.instaTools.models.repositories.AcceptorRepository
import com.pr0p1k.instaTools.models.repositories.AccessorRepository
import com.pr0p1k.instaTools.models.repositories.DonorRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import com.pr0p1k.instaTools.InstagramBean.AuthResult.Status.*

@RestController
class DataController {

    @Autowired
    lateinit var donorRepository: DonorRepository
    @Autowired
    lateinit var instagram: InstagramBean
    @Autowired
    lateinit var acceptorRepository: AcceptorRepository
    @Autowired
    lateinit var accessorRepository: AccessorRepository
    val mapper = ObjectMapper()

    @GetMapping("donors")
    fun donorList(): Iterable<Donor> {
        return donorRepository.findAll()
    }

    @GetMapping("acceptors")
    fun acceptorList(): Iterable<Acceptor> {
        return acceptorRepository.findAll()
    }

    @GetMapping("add")
    fun addDonor(@RequestParam login: String) {
        val json = instagram.getJsonNode(login)
        val profileInfo = json["entry_data"]["ProfilePage"][0]["graphql"]["user"]
        val id = profileInfo["id"].asText().toLong()
        val donor = Donor(id, login, !profileInfo["is_private"].asBoolean())
//        donor.posts.addAll(instagram.getPosts(id))
        donor.lastCheckDate = LocalDateTime.now()
        donorRepository.save(donor)
    }

    @GetMapping("open_donor")
    fun open(@RequestParam login: String): String {
        return instagram.getRawJson(login)
    }

    @GetMapping("test")
    fun lol(@RequestParam login: String): String = instagram.open(login)

    @GetMapping("add_acceptor")
    fun addAcceptor(@RequestParam name: String, @RequestParam url: String): Int {
        val new = Acceptor(name, url)
        acceptorRepository.save(new)
        return new.id
    }

    @GetMapping("add_accessor")
    fun addAccessor(@RequestParam login: String, @RequestParam password: String): ResponseEntity<String> {
        val response = { confirm: Boolean, auth: Boolean, user: Boolean ->
            mapper.createObjectNode()
                    .put("confirmation", confirm)
                    .put("authorized", auth)
                    .put("user", user)
        }
        return try {
            val authResult = instagram.authInInstagram(login, password)
            when (authResult.status) {
                SUCCESS -> {
                    accessorRepository.save(authResult.accessor!!)
                    ResponseEntity.ok(response(false, true, true).toString())
                }
                WRONG_PASSWORD -> ResponseEntity.ok(response(false, false, true).toString())
                WRONG_USERNAME -> ResponseEntity.ok(response(false, false, false).toString())
                NEED_CONFIRMATION -> ResponseEntity.ok(response(true, false, true).toString())
            }
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapper.createObjectNode()
                    .put("error", e.message).toString())
        }
    }

    @GetMapping("confirm_accessor")
    fun confirmAccessor(@RequestParam login: String, @RequestParam code: String) {
        val result = instagram.sendConfirmation(login, code) // TODO wrap
        if (result.accessor != null)
            accessorRepository.save(result.accessor)
    }

    @PostMapping("add_donors_accessor")
    fun addDonorsAcceptor(@RequestParam donor: String, @RequestParam accessor: String): ResponseEntity<String> {
        val donorObject = donorRepository.findByLogin(donor)
        val accessorObject = accessorRepository.findByLogin(accessor)
        if (!donorObject.isPresent || !accessorObject.isPresent)
            return ResponseEntity.status(404).body("{\"message\":\"Something wasn't found\"}")
        donorObject.get().accessors.add(accessorObject.get())
        donorRepository.save(donorObject.get())
        return ResponseEntity.ok("{\"message\":\"ok\"}")
    }

    @GetMapping("add_in_list")
    fun addDonorInList(@RequestParam id: Int, @RequestParam login: String): ResponseEntity<String> {
        val acceptor = acceptorRepository.findById(id)
        val donor = donorRepository.findByLogin(login)
        if (!donor.isPresent || !acceptor.isPresent)
            return ResponseEntity.status(404).body("{\"message\":\"Something wasn't found\"}")
        acceptor.get().listOfDonors.add(donor.get())
        acceptorRepository.save(acceptor.get())
        return ResponseEntity.ok("{\"message\":\"ok\"}")
    }

    @GetMapping("get_medias")
    fun getMedias(login: String): ResponseEntity<String> {
        val donor = donorRepository.findByLogin(login).orElseThrow { IllegalArgumentException() }
        val accessor = if (donor.open) {
            val i = (Math.random() * accessorRepository.count()).toInt()
            val iterator = accessorRepository.findAll().iterator()
            while (i > 0) iterator.next()
            iterator.next()
        } else donor.accessors.random()
        val medias = instagram.getMedias(donor, accessor)
        return if (medias.isPresent) ResponseEntity.ok(medias.get())
        else ResponseEntity.status(403).body("") // TODO
    }

//    @GetMapping("get_updates")
//    fun getUpdates(@RequestParam id: Int): String {
//        val updates = ObjectMapper().createArrayNode()
//        val list = acceptorRepository.findById(id).get().listOfDonors
//        for (donor in list) {
//            if (donor is Donor) {
//                val posts = instagram.getPosts(donor.login).filter { //TODO another method
//                    LocalDateTime.ofEpochSecond(
//                            it.date,
//                            0, ZoneOffset.UTC)
//                            .isAfter(donor.lastCheckDate)
//                }
//                updates.addAll(posts)
//            }
//        }
//        return updates.toString()
//    }

    //TODO fun for initial caching
}
