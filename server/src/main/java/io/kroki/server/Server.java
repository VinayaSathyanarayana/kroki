package io.kroki.server;

import io.kroki.server.action.Commander;
import io.kroki.server.error.ErrorHandler;
import io.kroki.server.service.Blockdiag;
import io.kroki.server.service.Bpmn;
import io.kroki.server.service.Bytefield;
import io.kroki.server.service.C4Plantuml;
import io.kroki.server.service.DiagramRegistry;
import io.kroki.server.service.DiagramRest;
import io.kroki.server.service.Ditaa;
import io.kroki.server.service.Erd;
import io.kroki.server.service.Excalidraw;
import io.kroki.server.service.Graphviz;
import io.kroki.server.service.HealthHandler;
import io.kroki.server.service.HelloHandler;
import io.kroki.server.service.Mermaid;
import io.kroki.server.service.Nomnoml;
import io.kroki.server.service.Plantuml;
import io.kroki.server.service.ServiceVersion;
import io.kroki.server.service.Svgbob;
import io.kroki.server.service.Umlet;
import io.kroki.server.service.Vega;
import io.kroki.server.service.Wavedrom;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Server extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigRetriever retriever = ConfigRetriever.create(vertx);
    retriever.getConfig(configResult -> {
      if (configResult.failed()) {
        startPromise.fail(configResult.cause());
      } else {
        start(vertx, configResult.result(), startResult -> {
          if (startResult.succeeded()) {
            startPromise.complete();
          } else {
            startPromise.fail(startResult.cause());
          }
        });
      }
    });
  }

  static void start(Vertx vertx, JsonObject config, Handler<AsyncResult<HttpServer>> listenHandler) {
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    BodyHandler bodyHandler = BodyHandler.create(false).setBodyLimit(config.getLong("KROKI_BODY_LIMIT", BodyHandler.DEFAULT_BODY_LIMIT));

    // CORS
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("Origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("Accept");
    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    router.route().handler(CorsHandler.create("*")
      .allowedHeaders(allowedHeaders)
      .allowedMethods(allowedMethods));

    Commander commander = new Commander(config);
    DiagramRegistry registry = new DiagramRegistry(router, bodyHandler);
    registry.register(new Plantuml(config), "plantuml");
    registry.register(new C4Plantuml(config), "c4plantuml");
    registry.register(new Ditaa(), "ditaa");
    registry.register(new Blockdiag(vertx, config), "blockdiag", "seqdiag", "actdiag", "nwdiag", "packetdiag", "rackdiag");
    registry.register(new Umlet(vertx), "umlet");
    registry.register(new Graphviz(vertx, config, commander), "graphviz", "dot");
    registry.register(new Erd(vertx, config, commander), "erd");
    registry.register(new Svgbob(vertx, config, commander), "svgbob");
    registry.register(new Nomnoml(vertx, config, commander), "nomnoml");
    registry.register(new Mermaid(vertx, config), "mermaid");
    registry.register(new Vega(vertx, config, Vega.SpecFormat.DEFAULT, commander), "vega");
    registry.register(new Vega(vertx, config, Vega.SpecFormat.LITE, commander), "vegalite");
    registry.register(new Wavedrom(vertx, config, commander), "wavedrom");
    registry.register(new Bpmn(vertx, config), "bpmn");
    registry.register(new Bytefield(vertx, config, commander), "bytefield");
    registry.register(new Excalidraw(vertx, config), "excalidraw");

    router.post("/")
      .handler(bodyHandler)
      .handler(new DiagramRest(registry).create());

    // health
    HealthHandler healthHandler = new HealthHandler();
    Handler<RoutingContext> healthHandlerService = healthHandler.create();
    router.get("/health")
      .handler(healthHandlerService);
    router.get("/v1/health") // versioned URL (alias)
      .handler(healthHandlerService);
    router.get("/healthz") // k8s liveness default URL (alias)
      .handler(healthHandlerService);

    // hello
    List<ServiceVersion> serviceVersions = healthHandler.getServiceVersions();
    String krokiBuildHash = healthHandler.getKrokiBuildHash();
    String krokiVersionNumber = healthHandler.getKrokiVersionNumber();
    router.get("/")
      .handler(new HelloHandler(vertx, serviceVersions, krokiVersionNumber, krokiBuildHash).create());

    // Default route
    Route route = router.route("/*");
    route.handler(routingContext -> routingContext.fail(404));
    route.failureHandler(new ErrorHandler(vertx, false));

    server.requestHandler(router).listen(config.getInteger("KROKI_PORT", 8000), listenHandler);
  }
}
