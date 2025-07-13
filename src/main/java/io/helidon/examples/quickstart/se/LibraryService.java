package io.helidon.examples.quickstart.se;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.http.NotFoundException;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.util.List;
import java.util.Random;

import io.helidon.common.context.Contexts;
import io.helidon.config.Config;

public class LibraryService implements HttpService {

  private final DbClient dbClient;

  LibraryService() {
    dbClient = Contexts.globalContext()
        .get(DbClient.class)
        .orElseGet(this::newDbClient);
    dbClient.execute()
        .namedDml("create-table");
  }

  private DbClient newDbClient() {
    return DbClient.create(Config.global().get("db"));
  }

  @Override
  public void routing(HttpRules rules) {
    rules
        .get("/book/{name}", this::getBook)
        .put("/book/{name}", this::addBook)
        .delete("/book/{name}", this::deleteBook)
        .get("/json/{name}", this::getJsonBook)
        .get("/benchmark/books", this::getBooks)
        .put("/benchmark/book", this::benchmarkAddBook);
  }

  private void getBook(ServerRequest request,
      ServerResponse response) {

    String bookName = request.path()
        .pathParameters()
        .get("name");

    String bookInfo = dbClient.execute()
        .namedGet("select-book", bookName)
        .map(row -> row.column("INFO").asString().get())
        .orElseThrow(() -> new NotFoundException(
            "Book not found: " + bookName));
    response.send(bookInfo);
  }

  private void addBook(ServerRequest request,
      ServerResponse response) {

    String bookName = request.path()
        .pathParameters()
        .get("name");

    String newValue = request.content().as(String.class);
    dbClient.execute()
        .createNamedInsert("insert-book")
        .addParam("name", bookName)
        .addParam("info", newValue)
        .execute();
    response.status(Status.CREATED_201).send();
  }

  private void deleteBook(ServerRequest request,
      ServerResponse response) {

    String bookName = request.path()
        .pathParameters()
        .get("name");

    dbClient.execute().namedDelete("delete-book", bookName);
    response.status(Status.NO_CONTENT_204).send();
  }

  private void getJsonBook(ServerRequest request,
      ServerResponse response) {

    String bookName = request.path()
        .pathParameters()
        .get("name");

    JsonObject bookJson = dbClient.execute()
        .namedGet("select-book", bookName)
        .map(row -> row.as(JsonObject.class))
        .orElseThrow(() -> new NotFoundException(
            "Book not found: " + bookName));
    response.send(bookJson);
  }

  private void getBooks(ServerRequest request,
      ServerResponse response) {

    JsonArrayBuilder jsonArrayBooks = Json.createArrayBuilder();
    List<JsonObject> booksJsonList = dbClient.execute()
        .namedQuery("select-books")
        .map(row -> row.as(JsonObject.class)).toList();
    for (JsonObject jsonObject : booksJsonList) {
      jsonArrayBooks.add(jsonObject);
    }
    response.send(jsonArrayBooks.build());
  }

  private void benchmarkAddBook(ServerRequest request,
      ServerResponse response) {

    dbClient.execute()
        .createNamedInsert("insert-book")
        .addParam("name", generateRandomString(10))
        .addParam("info", generateRandomString(20))
        .execute();
    response.status(Status.CREATED_201).send();
  }

  private String generateRandomString(int length) {
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder sb = new StringBuilder(length);
    Random random = new Random();

    for (int i = 0; i < length; i++) {
      int randomIndex = random.nextInt(characters.length());
      sb.append(characters.charAt(randomIndex));
    }

    return sb.toString();
  }
}
