package pw.hshen.hrpc.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import pw.hshen.hrpc.communication.model.RPCRequest;
import pw.hshen.hrpc.communication.model.RPCResponse;
import pw.hshen.hrpc.registry.ServiceDiscovery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@AllArgsConstructor
@Slf4j
public class RPCClientProxy {

    private ServiceDiscovery serviceDiscovery;

    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        // 创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String serviceAddress = "";

                        // 创建 RPC 请求对象并设置请求属性
                        RPCRequest request = new RPCRequest();
                        request.setRequestId(UUID.randomUUID().toString());
                        request.setInterfaceName(method.getDeclaringClass().getName());
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        request.setParameters(args);
                        // 获取 RPC 服务地址
                        if (serviceDiscovery != null) {
                            String serviceName = interfaceClass.getName();
                            serviceAddress = serviceDiscovery.discover(serviceName);
                            log.debug("discover service: {} => {}", serviceName, serviceAddress);
                        }
                        if (StringUtils.isEmpty(serviceAddress)) {
                            throw new RuntimeException("server address is empty");
                        }
                        // 从 RPC 服务地址中解析主机名与端口号
                        String[] array = StringUtils.split(serviceAddress, ":");
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        // 创建 RPC 客户端对象并发送 RPC 请求
                        RPCClient client = new RPCClient(host, port);
                        long time = System.currentTimeMillis();
                        RPCResponse response = client.send(request);
                        log.debug("time: {}ms", System.currentTimeMillis() - time);
                        if (response == null) {
                            throw new RuntimeException("response is null");
                        }
                        // 返回 RPC 响应结果
                        if (response.hasException()) {
                            throw response.getException();
                        } else {
                            return response.getResult();
                        }
                    }
                }
        );
    }
}
