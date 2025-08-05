package com.coc.cu.utils

import io.jsonwebtoken.*
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.client.RestTemplate
import java.util.*

class JwtUtils {
    val logger = LoggerFactory.getLogger(JwtUtils::class.java)

    private val jwtSecret: String = "topSecret"

    private val jwtExpirationMs = 24 * 5 * 60 * 60 * 1000


    fun generateJwtToken(authentication: Authentication): String {
        val userPrincipal: UserDetails = authentication.principal as UserDetails
        return Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact()
    }

    fun getUserNameFromJwtToken(token: String?): String {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .body
            .subject
    }

    fun validateJwtToken(authToken: String?): Boolean {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken)
            return true
        } catch (e: SignatureException) {
            logger.error("Invalid JWT signature: {} ", e.message)
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: {}", e.message)
        } catch (e: ExpiredJwtException) {
            logger.error("JWT token is expired: {} ", e.message)
        } catch (e: UnsupportedJwtException) {
            logger.error("JWT token is unsupported: {}", e.message)
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims string is empty: {}", e.message)
        }
        return false
    }

    fun sendSms(sender:String, to: String, message: String,restTemplate: RestTemplate): String? {
       return restTemplate.getForObject(
           "https://apps.mnotify.net/smsapi?key=WsdWfqH7Kr6fyiXDgLS25Ju62&to=${to}&msg=${message}&sender_id=${sender}",
           String::class.java
       )
    }


}