package com.coc.cu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Bean
    public fun userDetailsService(): UserDetailsService {
        val users: User.UserBuilder = User.withDefaultPasswordEncoder()
        val manager = InMemoryUserDetailsManager()
        manager.createUser(users.username("user").password("password").roles("USER").build())
        manager.createUser(users.username("admin").password("password").roles("USER","ADMIN").build())
        return manager
    }

    @Configuration
    @Order(1)
    class ApiWebSecurityConfigurationAdapter : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            http.authorizeRequests()
                .antMatchers(HttpMethod.GET,"/h2-console/**").permitAll()
                .antMatchers(HttpMethod.POST,"/h2-console/**").permitAll()
                .antMatchers(
                    "/swagger*",
                    "/api/v1/**",
                    "/oauth/authorize",
                    "/oauth/check_token",
                    "/oauth/token"
                ).permitAll()
                .and()
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .anonymous()

                .and()
                .csrf().disable()
                .cors()

                .and()
                .headers().frameOptions().disable()

                .and()
                .httpBasic().disable()

                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)


        }

    }
}