package pw.hshen.hrpc.sample.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import pw.hshen.hrpc.client.annotation.EnableRPCClients;

@EnableRPCClients(basePackages = {"pw.hshen.hrpc.sample.spi", "pw.hshen.hrpc.sample.spi_2"})
public class HelloClient {

	public static void main(String[] args) throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
		AnotherService anotherService = context.getBean(AnotherService.class);
		anotherService.callHelloService();
	}
}
