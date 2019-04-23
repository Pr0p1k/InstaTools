package com.pr0p1k.InstaTools

import org.hibernate.MappingException
import org.hibernate.dialect.identity.IdentityColumnSupportImpl
import org.hibernate.dialect.identity.IdentityColumnSupport
import org.apache.tomcat.jni.SSL.setPassword
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import javax.sql.DataSource


class SQLiteIdentityColumnSupport : IdentityColumnSupportImpl() {

    override fun supportsIdentityColumns(): Boolean {
        return true
    }

    @Throws(MappingException::class)
    override fun getIdentitySelectString(table: String?, column: String?, type: Int): String {
        return "select last_insert_rowid()"
    }

    @Throws(MappingException::class)
    override fun getIdentityColumnString(type: Int): String {
        return "integer"
    }
}
