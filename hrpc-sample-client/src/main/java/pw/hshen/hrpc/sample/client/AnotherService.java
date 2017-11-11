package pw.hshen.hrpc.sample.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pw.hshen.hrpc.sample.spi.HelloService;
import pw.hshen.hrpc.sample.spi_2.HelloService2;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hongbin
 * Created on 28/10/2017
 */
@Component
@Slf4j
public class AnotherService {
	@Autowired
	HelloService helloService;

	@Autowired
	HelloService2 helloService2;

	public void callHelloService() throws Exception {
		log.info("Result of callHelloService: {}", helloService.hello("world"));

		Thread.sleep(2000);
		log.info("Result of callHelloService: {}", helloService.hello("world"));

		log.info("Result of callHelloService2: {}", helloService2.hello("world"));
	}
}
