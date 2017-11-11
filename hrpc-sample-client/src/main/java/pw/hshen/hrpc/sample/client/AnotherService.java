package pw.hshen.hrpc.sample.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pw.hshen.hrpc.sample.spi.HelloService;

/**
 * @author hongbin
 * Created on 28/10/2017
 */
@Component
@Slf4j
public class AnotherService {
    @Autowired
    HelloService helloService;

    public void callHelloService() {
        log.info("Result of callHelloService: {}", helloService.hello("world"));
    }
}
