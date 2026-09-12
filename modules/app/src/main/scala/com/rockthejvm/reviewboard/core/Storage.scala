package com.rockthejvm.reviewboard.core

import zio.json.*

import org.scalajs.dom

object Storage {
  def set[A](key: String, value: A)(using encoder: JsonCodec[A]) =
    dom.window.localStorage.setItem(key, value.toJson)

  def get[A](key: String)(using decoder: JsonDecoder[A]): Option[A] =
    Option(dom.window.localStorage.getItem(key))
      .filter(!_.isEmpty())
      .flatMap(value => decoder.decodeJson(value).toOption)

  def remove(key: String): Unit =
    dom.window.localStorage.removeItem(key)
}
