package pw.hshen.hrpc.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
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
 * Created on 24/10/2017
 */
@Slf4j
@Data
public class ProxyFactoryBean implements FactoryBean<Object> {
    private Class<?> type;

    private ServiceDiscovery serviceDiscovery;

    @SuppressWarnings("unchecked")
    @Override
    public Object getObject() throws Exception {
        return Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
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
                        String serviceName = type.getName();
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

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
