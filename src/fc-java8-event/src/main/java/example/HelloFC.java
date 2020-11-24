package example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;
import com.aliyun.fc.runtime.PojoRequestHandler;


/**
 * Hello world!
 *
 */

public class HelloFC implements PojoRequestHandler<SimpleRequest, SimpleResponse> {

    @Override
    public SimpleResponse handleRequest(SimpleRequest request, Context context) {
        String message = "Hello, " + request.getFirstName() + " " + request.getLastName();
        return new SimpleResponse(message);
    }
} 

