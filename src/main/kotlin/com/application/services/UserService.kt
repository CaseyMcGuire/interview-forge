package com.application.services

import com.application.dao.UserDao
import com.application.exceptions.UserAlreadyExistsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
  private val userDao: UserDao,
  private val passwordEncoder: PasswordEncoder
) {

  fun getUserByUsername(email: String): User? {
    return userDao.findByEmail(email)
  }

  fun createUser(email: String, password: String): User {
    val existingUser = userDao.findByEmail(email)
    if (existingUser != null) {
      throw UserAlreadyExistsException("A user with that username already exists")
    }
    val hashedPassword = passwordEncoder.encode(password)
      ?: throw IllegalStateException("Password encoder returned null")
    return userDao.createUser(email, hashedPassword)
  }
}