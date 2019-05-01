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
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL


@Component
class InstagramBean {
    val loginUrl = "https://www.instagram.com/accounts/login/ajax/"
    val mediasUrl = "https://instagram.com/graphql/query/?query_id=17888483320059182&id={{userId}}&first={{count}}&after={{maxId}}"
    val baseUrl = "https://www.instagram.com"
    var csrfToken = ""
    var rolloutHash = ""
    var logger = LoggerFactory.getLogger(InstagramBean::class.java)

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
     * Opens instagram page with or without credentials and returns flat html
     * @return string with html
     */
    fun open(login: String, sessionId: String? = null): String {
        var connection = URL("https://instagram.com/$login").openConnection() as HttpURLConnection
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
     * Loads tokens from the instagram page
     */
    fun loadTokens() {
        val request = URL(baseUrl).openConnection() as HttpURLConnection
        val response = readContent(request)
        if (this.csrfToken.isEmpty())
            this.csrfToken = response.substringAfter("\"csrf_token\":\"").substring(0, 32)
        else if (this.rolloutHash.isEmpty())
            this.rolloutHash = response.substringAfter("\"rollout_hash\":\"", "1").substring(0, 12)
    }

    /**
     * Authorizes in instagram and returns new {@see Accessor} object
     */
    fun authInInstagram(login: String, password: String): Accessor {
        loadTokens()
        logger.info("Tokens: csrf: $csrfToken rollout: $rolloutHash")
        val connection = URL(loginUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.addRequestProperty("username", login)
        connection.addRequestProperty("password", password)
        connection.addRequestProperty("Referer", "$baseUrl/accounts/login/?source=auth_switcher")
        connection.addRequestProperty("X-CSRFToken", csrfToken)
        connection.addRequestProperty("X-Instagram-AJAX", rolloutHash)
        if (connection.responseCode / 100 != 2)
            throw ConnectException("${connection.responseCode}: ${connection.responseMessage}")
        val response = readContent(connection)
        logger.info("Cookies: ${connection.getHeaderField("Set-Cookie")} response: $response")
        // TODO responds with user:false. dunno why
        return Accessor(login, connection.getHeaderField("Set-Cookie"))
    }
}
