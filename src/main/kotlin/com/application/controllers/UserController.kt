package com.application.controllers

import com.application.exceptions.UserAlreadyExistsException
import com.application.services.UserService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class UserController(
  private val userService: UserService,
) {

  @PostMapping("/user")
  fun createUser(
    @RequestParam email: String,
    @RequestParam password: String
  ): String {
    try {
      val user = userService.createUser(email, password)
    } catch (ex: UserAlreadyExistsException) {
      return "redirect:/register?error=user_already_exists"
    }
    return "redirect:/login"
  }
}

data class User(val email: String, val password: String)