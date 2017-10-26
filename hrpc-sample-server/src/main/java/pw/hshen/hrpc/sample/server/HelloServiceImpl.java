package pw.hshen.hrpc.sample.server;

import pw.hshen.hrpc.sample.spi.HelloService;
import org.springframework.stereotype.Component;

@Component
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

}
