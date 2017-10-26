package pw.hshen.hrpc.sample.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBootstrap {

    public static void main(String[] args) {
        System.out.println("start server");
        new ClassPathXmlApplicationContext("spring.xml");
    }
}
