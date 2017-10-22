package client;

import pw.hshen.hrpc.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import pw.hshen.hrpc.client.RPCClientProxy;

public class HelloClient {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        RPCClientProxy rpcProxy = context.getBean(RPCClientProxy.class);

        HelloService helloService = rpcProxy.create(HelloService.class);
        String result = helloService.hello("World");
        System.out.println(result);
    }
}
