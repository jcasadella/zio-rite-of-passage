package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.UserId
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.http.responses.UserResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.* // imports the type class derivation package
import sttp.tapir.json.zio.*

trait UserEndpoints extends BaseEndpoint {

  val createUserEndpoint = baseEndpoint
  .tag("Users")
  .name("register")
  .description("Registar a user account with email and password")
  .in("users")
  .post
  .in(jsonBody[RegisterUserAccount])
  .out(jsonBody[UserResponse])

  val updatePasswordEndpoint =
    secureBaseEndpoint
    .tag("Users")
    .name("update password")
    .description("Update user password")
    .in("users" / "password")
    .put
    .in(jsonBody[UpdatePasswordRequest]) 
    .out(jsonBody[UserResponse])

  val deleteUserEndpoint =
    secureBaseEndpoint
    .tag("Users")
    .name("delete")
    .description("Delete user account")
    .in("users")
    .delete
    .in(jsonBody[DeleteAccountRequest])
    .out(jsonBody[UserResponse])
  
  val loginEndpoint =
    baseEndpoint
    .tag("Users")
    .name("login")
    .description("Log in and generate a JWT token")
    .in("users" / "login")
    .post
    .in(jsonBody[LoginRequest])
    .out(jsonBody[UserToken])

  val forgotPasswordEndpoint =
    baseEndpoint
    .tag("Users")
    .name("forgot password")
    .description("Trigger email for password recovery")
    .in("users" / "forgot")
    .post
    .in(jsonBody[ForgotPasswordRequest])

  val recoverPasswordEndpoint =
    baseEndpoint
    .tag("Users")
    .name("recover password")
    .description("Set new password based on OTP")
    .in("users" / "recover")
    .post
    .in(jsonBody[RecoverPasswordRequest])
}
