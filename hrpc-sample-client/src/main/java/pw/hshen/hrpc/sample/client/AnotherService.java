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
		while (true) {
			try {
				Thread.sleep(1000);
				log.info("call hello service");
				log.debug("Result of callHelloService: {}", helloService.hello("world"));
			} catch (Exception e) {
				log.error("error: ", e);
			}
		}
	}
}
