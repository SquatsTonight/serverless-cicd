package example;

import static org.junit.Assert.*;
import org.junit.Test;

public class HelloFCTest {
  @Test
  public void testHandleRequest() {
    SimpleRequest req = new SimpleRequest("FC", "aliyun");
    String message = "Hello, " + req.getFirstName() + " " + req.getLastName();
    SimpleResponse res = new SimpleResponse(message);
    assertEquals(res.getMessage(), new HelloFC().handleRequest(req, null).getMessage());
  }
}