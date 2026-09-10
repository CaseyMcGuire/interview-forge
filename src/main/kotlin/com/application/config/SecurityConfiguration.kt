package com.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler

// Authentication is wired up by Spring Security automatically: because a single UserDetailsService bean
// (UserDetailsServiceImpl) and a PasswordEncoder bean are present, it configures a DaoAuthenticationProvider
// and AuthenticationManager for us. No need to build one by hand from the HttpSecurity shared object.
@EnableWebSecurity
@Configuration
class SecurityConfiguration {

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {

    // Use the plain CsrfTokenRequestAttributeHandler (not the default XOR-based one)
    // to produce a stable CSRF token compatible with the SPA frontend.
    val requestHandler = CsrfTokenRequestAttributeHandler()
    http {
      authorizeHttpRequests {
        authorize("/**", permitAll)
      }
      formLogin {
        loginPage = "/login"
        defaultSuccessUrl("/", true)
        failureUrl = "/login?error=true"
        // note usernameParameter and password parameter aren't available yet but they will be at some point:
        // https://github.com/spring-projects/spring-security/issues/14474
      }
      csrf {
        csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
        csrfTokenRequestHandler = requestHandler
      }
      // Writes the XSRF-TOKEN cookie on the initial page load so the SPA's first
      // mutation/query doesn't 403 (see CsrfCookieFilter)
      addFilterAfter<BasicAuthenticationFilter>(CsrfCookieFilter())
      logout {
        logoutSuccessUrl = "/"
      }
    }

    return http.build();
  }
}