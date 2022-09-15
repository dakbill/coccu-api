package com.coc.cu.config

import com.coc.cu.filters.AuthTokenFilter
import com.coc.cu.utils.JwtUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsUtils
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebSecurityConfiguration(val jwtUtils: JwtUtils, val userDetailsService: UserDetailsService) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
            .antMatchers(HttpMethod.GET, "/h2-console/**").permitAll()
            .antMatchers(HttpMethod.POST, "/h2-console/**").permitAll()
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


            .and()
            .addFilterBefore(
                AuthTokenFilter(jwtUtils, userDetailsService),
                UsernamePasswordAuthenticationFilter::class.java
            )
    }
}

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
class WebMVCSecurityConfiguration : WebMvcConfigurer {
    @Value("\${cors.allowedOrigins}")
    private val allowedOrigins: Array<String>? = null

    override fun addCorsMappings(registry: CorsRegistry) {
        if (allowedOrigins != null) {
            registry.addMapping("/**")
                .allowedMethods("*")
                .allowedOriginPatterns(*allowedOrigins)
                .allowCredentials(true)
        }
    }
}







