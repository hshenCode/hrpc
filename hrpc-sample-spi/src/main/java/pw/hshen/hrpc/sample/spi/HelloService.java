package pw.hshen.hrpc.sample.spi;

import pw.hshen.hrpc.common.annotation.RPCService;

@RPCService(HelloService.class)
public interface HelloService {

	String hello(String name);
}
