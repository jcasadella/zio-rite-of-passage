package com.rockthejvm.reviewboard.http.controllers

import zio.*

import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.errors.UnauthorizedException
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.services.JwtService
import com.rockthejvm.reviewboard.services.UserService

class UserController private (userService: UserService, jwtService: JwtService)
    extends Controller
    with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint
    .serverLogic { req =>
      userService
        .registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] = updatePasswordEndpoint
    .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .updatePassword(req.email, req.oldPassword, req.newPassword)
        .map(user => UserResponse(user.email))
        .either
    }

  val delete: ServerEndpoint[Any, Task] = deleteUserEndpoint
    .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .delete(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val forgotPassword: ServerEndpoint[Any, Task] = forgotPasswordEndpoint
    .serverLogic { req =>
      userService.sendPasswordRecoveryToken(req.email).either
    }

  val recoverPassword: ServerEndpoint[Any, Task] = recoverPasswordEndpoint
    .serverLogic { req =>
      userService
        .recoverPasswordFromToken(req.email, req.token, req.newPassword)
        .filterOrFail(b => b)(UnauthorizedException)
        .unit
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    login,
    updatePassword,
    delete,
    forgotPassword,
    recoverPassword
  )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JwtService]
  } yield new UserController(userService, jwtService)
}
