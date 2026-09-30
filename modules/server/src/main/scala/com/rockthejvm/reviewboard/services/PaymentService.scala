package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.Configs
import com.rockthejvm.reviewboard.config.StripeConfig
import com.stripe.Stripe as TheStripe
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import zio.*

import scala.jdk.OptionConverters.*

trait PaymentService {
  def createCheckoutSession(invitePackId: Long, userEmail: String): Task[Option[Session]]
  def handleWebhookEvent[A](
    signature: String,
    payload: String,
    action: String => Task[A]
  ): Task[Option[A]]
}

class PaymentServiceLive private (config: StripeConfig) extends PaymentService {
  
  def createCheckoutSession(invitePackId: Long, userEmail: String): Task[Option[Session]] =
    ZIO.attemptBlocking {
      SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setSuccessUrl(config.successUrl)
        .setCancelUrl(config.cancelUrl)
        .setCustomerEmail(userEmail)
        .setClientReferenceId(invitePackId.toString) // payload that will return in the webhook
        .setInvoiceCreation( // optionally
          SessionCreateParams.InvoiceCreation
            .builder()
            .setEnabled(true)
            .build()
        )
        .setPaymentIntentData( // optionally send email to user
          SessionCreateParams.PaymentIntentData
            .builder()
            .setReceiptEmail(userEmail)
            .build()
        )
        .addLineItem(
          SessionCreateParams.LineItem
            .builder()
            .setPrice(config.invitePackPriceId)
            .setQuantity(1L)
            .build()
        )
        .build()
    }
    .map(params => Session.create(params))
    .map(Option(_))
    .logError("Stripe session creation FAILED")
    .catchSome {
      case _ => ZIO.none
    }

  override def handleWebhookEvent[A](
    signature: String,
    payload: String,
    action: String => Task[A]
  ): Task[Option[A]] =
    ZIO.attempt {
      Webhook.constructEvent(payload, signature, config.invitesWebhookSecret)
    }
    .flatMap { event =>
      event.getType() match {
        case "checkout.session.completed" =>
          ZIO.foreach(
            event.getDataObjectDeserializer()
              .getObject()
              .toScala
              .map(_.asInstanceOf[Session])
              .map(_.getClientReferenceId())
          )(action)
        case _ =>
          ZIO.none
      }
    }
}

object PaymentServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[StripeConfig]
      _ <- ZIO.attempt(TheStripe.apiKey = config.secretKey)
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer =
    Configs.makeLayer[StripeConfig]("rockthejvm.stripe") >>> layer
}
