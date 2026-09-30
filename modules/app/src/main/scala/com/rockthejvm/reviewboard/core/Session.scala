package com.rockthejvm.reviewboard.core

import scala.scalajs.js.Date

import com.rockthejvm.reviewboard.domain.data.UserToken

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object Session {

  val stateKey: String = "userState"

  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive: Boolean = {
    loadUserState()
    userState.now().nonEmpty
  }

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set(stateKey, token)
  }

  def loadUserState(): Unit = {
    // Clean storage if token is expired
    Storage
      .get[UserToken](stateKey)
      .filter(_.expires * 1000 <= new Date().getTime())
      .foreach(_ => Storage.remove(stateKey))

    // retrieve the user token
    val currentToken = Storage.get[UserToken](stateKey)
    if (userState.now() != currentToken)
      userState.set(currentToken)
  }

  def clearUserState(): Unit = {
    Storage.remove(stateKey)
    userState.set(Option.empty)
  }

  def getUserState: Option[UserToken] = {
    loadUserState()
    userState.now()
  }
}
