package com.application.db.models

import com.application.services.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserDetailsImpl(
  val user: User
) : UserDetails {

  override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
    if (user.role == null) {
      return mutableListOf()
    }
    return mutableListOf(SimpleGrantedAuthority("ROLE_${user.role}"))
  }

  override fun isEnabled(): Boolean {
    return true
  }

  override fun getUsername(): String {
    return user.username
  }

  override fun isCredentialsNonExpired(): Boolean {
    return true
  }

  override fun getPassword(): String {
    return user.hashedPassword
  }

  override fun isAccountNonExpired(): Boolean {
    return true
  }

  override fun isAccountNonLocked(): Boolean {
    return true
  }
}