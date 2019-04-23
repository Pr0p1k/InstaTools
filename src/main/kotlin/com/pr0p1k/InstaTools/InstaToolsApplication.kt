package com.pr0p1k.InstaTools

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@SpringBootApplication
class InstaToolsApplication

fun main(args: Array<String>) {
	runApplication<InstaToolsApplication>(*args)
}
