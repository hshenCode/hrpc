import pw.hshen.hrpc.sample.spi.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import pw.hshen.hrpc.common.annotation.EnableRPCClients;

@EnableRPCClients(basePackages = {"pw.hshen.hrpc"})
public class HelloClient {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");

        HelloService helloService = context.getBean(HelloService.class);
        String result = helloService.hello("World");
        System.out.println(result);
    }
}
