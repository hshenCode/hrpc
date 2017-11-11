package pw.hshen.hrpc.sample.spi_2;

import pw.hshen.hrpc.common.annotation.RPCService;

@RPCService(HelloService2.class)
public interface HelloService2 {

	String hello(String name);
}
