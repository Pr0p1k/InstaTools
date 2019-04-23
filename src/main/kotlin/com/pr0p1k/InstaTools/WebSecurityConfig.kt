package com.pr0p1k.InstaTools

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@EnableWebSecurity
class WebSecurityConfig: WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .formLogin()
                .and()
                .httpBasic()
    }
}
