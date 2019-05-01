package com.pr0p1k.instaTools.controllers

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
        try {
            val accessor = instagram.authInInstagram(login, password)
            accessorRepository.save(accessor)
        } catch (e: Exception) {
            return ResponseEntity.status(500).body("{\"message\":\"${e.message}\"}")
        }
        return ResponseEntity.ok("{\"message\":\"ok\"}")
    }

    @PostMapping("add_donors_accessor")
    fun addDonorsAcceptor(@RequestParam donor: String, @RequestParam accessor: String): ResponseEntity<String> {
        val donor = donorRepository.findByLogin(donor)
        val accessor = accessorRepository.findByLogin(accessor)
        if (donor.isEmpty || accessor.isEmpty)
            return ResponseEntity.status(404).body("{\"message\":\"Something wasn't found\"}")
        donor.get().accessors.add(accessor.get())
        donorRepository.save(donor.get())
        return ResponseEntity.ok("{\"message\":\"ok\"}")
    }

    @GetMapping("add_in_list")
    fun addDonorInList(@RequestParam id: Int, @RequestParam login: String): ResponseEntity<String> {
        val acceptor = acceptorRepository.findById(id)
        val donor = donorRepository.findByLogin(login)
        if (donor.isEmpty || acceptor.isEmpty)
            return ResponseEntity.status(404).body("{\"message\":\"Something wasn't found\"}")
        acceptor.get().listOfDonors.add(donor.get())
        acceptorRepository.save(acceptor.get())
        return ResponseEntity.ok("{\"message\":\"ok\"}")
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
