package castservice

import java.io.ObjectInputStream
import java.net.Socket
import java.rmi.Naming

object ScalaClient {

  def main(args: Array[String]): Unit = {

    if (args.length < 1) {
      System.err.println(
        "Argument missing: java castservice.Main <service host address>")
      System.exit(2)
    }
    val service_host: java.lang.String = args(0)
    val a: Cast = new Cast(1L, 2L, 0, 3.0f, 4, 1, Array(10L, 11L, 12L, 13L))
    val b: Cast = new Cast(1L, 2L, 0, 3.0f, 4, 0, Array(10L, 11L, 12L, 13L))
    val exit_code: Int = 0
    val client: Client = new Client(service_host)
    client.service().sendCast(a)
    var casts: Array[Cast] = client.service().getActiveCasts(10)
    assert((casts.length == 1 && casts(0).id() == a.id()))
    casts = client.service().getActiveCasts(11)
    assert((casts.length == 1 && casts(0).id() == a.id()))
    client.service().cancelCast(a.originatorUserId_, a.bondId_, a.side_)
    casts = client.service().getActiveCasts(10)
    assert((casts.length == 0))
    casts = client.service().getActiveCasts(11)
    assert((casts.length == 0))
    client.service().sendCast(a)
    client.service().sendCast(b)
    casts = client.service().getActiveCasts(10)
    assert((casts.length == 0))
// streaming
    val port: Int = client.service().streamCasts(10)
    val sock: Socket = new Socket(service_host, port)
    val stream: ObjectInputStream = new ObjectInputStream(sock.getInputStream)
    client.service().sendCast(a)
    for (i <- 1.until(10)) {
      val cast: Cast = stream.readObject().asInstanceOf[Cast]
      println(cast)
      client.service().sendCast(a)
      Thread.sleep(10)
    }
// timing sendCast() burst
    println("timing sendCast() burst - 1000 calls")
    var t0: Long = System.currentTimeMillis()
    for (i <- 1.until(1000)) {
      { a.bondId_ += 1; a.bondId_ - 1 }
      client.service().sendCast(a)
    }
    var t1: Long = System.currentTimeMillis()
    printf("%.3f ms per sendCast()\n", (t1 - t0) / 1000.0)
// timing sendCast() burst
    println("timing sendCast() burst - 1000 calls")
    t0 = System.currentTimeMillis()
    for (i <- 1.until(1000)) {
      client
        .service()
        .cancelCast(a.originatorUserId_, { a.bondId_ -= 1; a.bondId_ + 1 },
                    a.side_)
    }
    t1 = System.currentTimeMillis()
    printf("%.3f ms per cancelCast()\n", (t1 - t0) / 1000.0)
// timing sendCast() burst
    println("timing sendCast() burst - 1000 calls")
    t0 = System.currentTimeMillis()
    for (i <- 1.until(1000)) {
      casts = client.service().getActiveCasts(11)

    }
    t1 = System.currentTimeMillis()
    printf("%.3f ms per getActiveCasts()\n", (t1 - t0) / 1000.0)
    println("Exiting...")
  }

}


class ScalaClient(host_addr: String) {

  val addr: String = s"rmi://$host_addr:${ICastService.REGISTRY_PORT}/${ICastService.RMI_NAME}"

  private val service_ : ICastService = Naming.lookup(addr).asInstanceOf[ICastService]

  def service(): ICastService = service_
}
