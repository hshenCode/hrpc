package pw.hshen.server;

import pw.hshen.hrpc.HelloService;
import pw.hshen.hrpc.server.annotation.RPCService;

@RPCService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

}
