package com.pr0p1k.instaTools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.pr0p1k.instaTools.models.Accessor
import com.pr0p1k.instaTools.models.Post
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import kotlin.math.log


@Component
class InstagramBean {
    val loginUrl = "https://www.instagram.com/accounts/login/ajax/"
    val confirmationUrl = "https://www.instagram.com/challenge/10742181112/im72yMstkQ/" // TODO trouble
    val mediasUrl = "https://instagram.com/graphql/query/?query_id=17888483320059182&id={{userId}}&first={{count}}&after={{maxId}}"
    val baseUrl = "https://www.instagram.com"
    var logger = LoggerFactory.getLogger(InstagramBean::class.java)
    val mapper = ObjectMapper()

    /**
     * Writes page's content to StringBuilder
     */
    private fun readContent(connection: HttpURLConnection): String {
        val input = BufferedReader(InputStreamReader(connection.inputStream))
        val content = java.lang.StringBuilder()
        input.forEachLine {
            content.append(it)
        }
        input.close()
        return content.toString()
    }

    /**
     * Put properties into a new connection object and return it.
     */
    private fun prepareConnection(url: String, method: String, body: ByteArray = byteArrayOf(), vararg params: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            for (i in 0 until params.size step 2)
                addRequestProperty(params[i], params[i + 1])
            if (body.isNotEmpty()) {
                doOutput = true
                outputStream.write(body)
            }
        }
    }

    /**
     * Opens instagram page with or without credentials and returns flat html
     * @return string with html
     */
    fun open(login: String, sessionId: String? = null): String {
        val connection = URL("https://instagram.com/$login").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (sessionId != null) {
            connection.setRequestProperty("Cookie", sessionId)
        }

        if (connection.responseCode != 200) {
            throw ConnectException("${connection.responseCode}: ${connection.responseMessage}")
        }
        return readContent(connection)
    }

    /**
     * Returns string representing json of account's content
     */
    fun getRawJson(login: String): String {
        return Jsoup
                .parse(open(login))
                .body()
                .getElementsByTag("script")[0]
                .data().substringAfter("= ")
    }

    fun getPosts(id: Long, sessionId: String, count: Int = 100, maxId: String = ""): List<Post> {
        val posts = mutableListOf<Post>()
        val connection = URL(mediasUrl.replace("{{userId}}", id.toString())
                .replace("{{count}}", count.toString())
                .replace("{{maxId}}", maxId)).openConnection() as HttpURLConnection
        connection.addRequestProperty("sessionid", sessionId)
        // TODO maybe more headers
        return posts
    }

    fun getJsonNode(login: String): JsonNode {
        return ObjectMapper().readTree(getRawJson(login))
    }

    /**
     * Gets tokens from the instagram page
     * @return pair of csrfToken and rolloutHash
     */
    private fun getTokens(): Pair<String, String> {
        val request = URL(baseUrl).openConnection() as HttpURLConnection
        val response = readContent(request)
        return Pair(response.substringAfter("\"csrf_token\":\"").substring(0, 32),
                response.substringAfter("\"rollout_hash\":\"", "1").substring(0, 12))

    }

    /**
     * Accesses inst and gets sessionId for new [Accessor] object and puts it in database
     * If response code is 200, checks, whether user is really authorized. (user and authorized in response are true) otherwise TODO
     * If response code is 400, asks for code TODO
     */
    fun authInInstagram(login: String, password: String): AuthResult {
        val (csrfToken, rolloutHash) = getTokens()
        logger.info("Tokens: csrf: $csrfToken rollout: $rolloutHash")
        val connection = prepareConnection(loginUrl, "POST", ("username=$login&password=$password" +
                "&queryParams=%7B%22source%22%3A%22auth_switcher%22%7D&optIntoOneTap=true").toByteArray(),
                "Referer", "$baseUrl/",
                "X-CSRFToken", csrfToken,
                "X-Instagram-AJAX", rolloutHash,
                "Content-Type", "application/x-www-form-urlencoded")

        when (connection.responseCode) {
            200 -> {
                val response = readContent(connection)
                logger.info("Cookies: ${connection.getHeaderField("Set-Cookie")}\nresponse: $response")
                val responseNode = mapper.readTree(response)
                return when {
                    !responseNode["user"].asBoolean() -> AuthResult(null, AuthResult.Status.WRONG_USERNAME)
                    !responseNode["authenticated"].asBoolean() -> AuthResult(Accessor(login, csrfToken, rolloutHash), AuthResult.Status.WRONG_PASSWORD)
                    else -> {
                        // TODO put csrf and hash into an object an save it
                        val sessionId = connection.getHeaderField("Set-Cookie").substringAfter("sessionid=").substringBefore(";")
                        // expiration date is now + 364 days
                        AuthResult(Accessor(login, csrfToken, rolloutHash, sessionId, LocalDateTime.now().plusDays(364)), AuthResult.Status.SUCCESS)
                    }
                }

            }
            400 -> Accessor(login, csrfToken, rolloutHash).apply {
                Accessor.confirmationQueue.put(login, this) // TODO not accessor, but what?
                prepareConnection(confirmationUrl, "GET",
                        params = *arrayOf("referer", confirmationUrl, "X-CSRFToken", csrfToken,
                                "X-Instagram-AJAX", rolloutHash, "Content-Type", "application/x-www-form-urlencoded")).responseCode // TODO ?
                prepareConnection(confirmationUrl, "POST", "choice: 1".toByteArray(),
                        params = *arrayOf("referer", confirmationUrl, "X-CSRFToken", csrfToken,
                                "X-Instagram-AJAX", rolloutHash, "Content-Type", "application/x-www-form-urlencoded")).responseCode
                return AuthResult(this, AuthResult.Status.NEED_CONFIRMATION)
            } // TODO would it even work
            else -> throw ConnectException("${connection.responseCode}: ${connection.responseMessage}")
        }
    }

    fun sendConfirmation(login: String, code: String): AuthResult {
        val accessor = Accessor.confirmationQueue.remove(login) ?: throw IllegalArgumentException("invalid login")
        val connection = prepareConnection(confirmationUrl, "POST", "security_code: $code".toByteArray(),
                params = *arrayOf("referer", confirmationUrl, "X-CSRFToken", accessor.csrfToken,
                        "X-Instagram-AJAX", accessor.rolloutHash, "Content-Type", "application/x-www-form-urlencoded"))
        return when (connection.responseCode) {
            200 -> {
                accessor.sessionId = connection.getHeaderField("Set-Cookie")
                        .substringAfter("sessionid=").substringBefore(";")
                AuthResult(accessor, AuthResult.Status.SUCCESS)
            }
            else -> AuthResult(accessor, AuthResult.Status.NEED_CONFIRMATION)
        }
    }

    /**
     * Gets medias of an account/tag/etc. Decides whether to use accessor or not
     */
    fun getMedias(): AccessResult {

        TODO()
    }

    data class AccessResult(val status: Status) {
        enum class Status {
            SUCCESS,
            RESTRICTED
        }
    }

    data class AuthResult(val accessor: Accessor?, val status: Status) {
        enum class Status {
            SUCCESS,
            WRONG_PASSWORD,
            WRONG_USERNAME,
            NEED_CONFIRMATION
        }
    }
}

//First query, that returns page. GET
//https://www.instagram.com/challenge/10742181112/im72yMstkQ/
//referer: ^
//
//
//# Second. POST
//https://www.instagram.com/challenge/10742181112/im72yMstkQ/
//referer: ^
//body: choice: 1
//
//# Third. POST
//https://www.instagram.com/challenge/10742181112/im72yMstkQ/
//referer: ^
//body: security_code:
