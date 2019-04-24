package com.pr0p1k.instaTools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.pr0p1k.instaTools.models.Accessor
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL

@Component
class InstagramBean {
    val LOGIN_URL = "https://www.instagram.com/accounts/login/ajax/"

    /**
     * Writes page's content to StringBuilder
     */
    private fun loadContent(connection: HttpURLConnection, content: StringBuilder) {
        val input = BufferedReader(InputStreamReader(connection.inputStream))
        input.forEachLine {
            content.append(it)
        }
        input.close()
    }

    /**
     * Opens instagram page with or without credentials and returns flat html
     * @return string with html
     */
    fun open(login: String, sessionId: String? = null): String {
        var connection = URL("https://instagram.com/$login/").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        if (sessionId != null) {
            connection.setRequestProperty("Cookie", sessionId)
        }
        val responseCode = connection.responseCode
        val content = StringBuilder()
        when (responseCode) {
            301, 302 -> {
                connection = URL(connection.getHeaderField("Location")).openConnection() as HttpURLConnection
                loadContent(connection, content)
            }
            200 -> {
                loadContent(connection, content)
            }
            else -> {
                throw ConnectException("${connection.responseCode}: ${connection.responseMessage}")
            }
        }
        return content.toString()
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

    fun getJsonNode(login: String): JsonNode {
        return ObjectMapper().readTree(getRawJson(login))
    }

    /**
     * Authorizes in instagram and returns new {@see Accessor} object
     */
    fun authInInstagram(login: String, password: String): Accessor {
        val connection = URL(LOGIN_URL).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.addRequestProperty("username", login)
        connection.addRequestProperty("password", password)
        // read the response and put in new object
        return Accessor(login, "TODO")  // TODO
    }
}
