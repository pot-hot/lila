package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.relay.{ Relay => RelayModel }
import views._

object Relay extends LilaController {

  private def env = Env.relay

  private def relayNotFound(implicit ctx: Context) = NotFound(html.relay.notFound())

  val index = Open { implicit ctx =>
    env.repo recent 30 map { relays =>
      Ok(html.relay.home(relays))
    }
  }

  def show(id: String, slug: String) = Open { implicit ctx =>
    env.repo byId id flatMap {
      _.fold(relayNotFound.fuccess) { relay =>
        if (relay.slug != slug) Redirect(routes.Relay.show(id, relay.slug)).fuccess
        else env.version(relay.id) zip env.jsonView(relay) zip chatOf(relay) map {
          case ((version, data), chat) => html.relay.show(relay, version, data, chat)
        }
      } map NoCache
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    (getInt("version") |@| get("sri")).tupled ?? {
      case (version, uid) => env.socketHandler.join(id, version, uid, ctx.me)
    }
  }

  private def chatOf(relay: RelayModel)(implicit ctx: Context) =
    ctx.isAuth ?? {
      Env.chat.api.userChat find relay.id map (_.forUser(ctx.me).some)
    }
}
