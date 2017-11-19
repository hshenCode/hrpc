package pw.hshen.hrpc.client.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;
import pw.hshen.hrpc.client.ChannelManager;
import pw.hshen.hrpc.client.RPCResponseFuture;
import pw.hshen.hrpc.client.ResponseFutureManager;
import pw.hshen.hrpc.common.model.RPCRequest;
import pw.hshen.hrpc.common.model.RPCResponse;
import pw.hshen.hrpc.registry.ServiceDiscovery;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FactoryBean for service proxy
 *
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
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this::doInvoke);
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
		String targetServiceName = type.getName();

		// Create request
		RPCRequest request = RPCRequest.builder()
				.requestId(generateRequestId(targetServiceName))
				.interfaceName(method.getDeclaringClass().getName())
				.methodName(method.getName())
				.parameters(args)
				.parameterTypes(method.getParameterTypes()).build();

		// Get service address
		InetSocketAddress serviceAddress = getServiceAddress(targetServiceName);

		// Get channel by service address
		Channel channel = ChannelManager.getInstance().getChannel(serviceAddress);
		if (null == channel) {
			throw new RuntimeException("Cann't get channel for address" + serviceAddress);
		}

		// Send request
		RPCResponse response = sendRequest(channel, request);
		if (response == null) {
			throw new RuntimeException("response is null");
		}
		if (response.hasException()) {
			throw response.getException();
		} else {
			return response.getResult();
		}
	}

	private String generateRequestId(String targetServiceName) {
		return targetServiceName + "-" + UUID.randomUUID().toString();
	}

	private InetSocketAddress getServiceAddress(String targetServiceName) {
		String serviceAddress = "";
		if (serviceDiscovery != null) {
			serviceAddress = serviceDiscovery.discover(targetServiceName);
			log.debug("Get address: {} for service: {}", serviceAddress, targetServiceName);
		}
		if (StringUtils.isEmpty(serviceAddress)) {
			throw new RuntimeException(String.format("Address of target service %s is empty", targetServiceName));
		}
		String[] array = StringUtils.split(serviceAddress, ":");
		String host = array[0];
		int port = Integer.parseInt(array[1]);
		return new InetSocketAddress(host, port);
	}

	private RPCResponse sendRequest(Channel channel, RPCRequest request) {
		CountDownLatch latch = new CountDownLatch(1);
		RPCResponseFuture rpcResponseFuture = new RPCResponseFuture(request.getRequestId());
		ResponseFutureManager.getInstance().registerFuture(rpcResponseFuture);
		channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}

		try {
			// TODO: make timeout configurable
			return rpcResponseFuture.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.warn("Exception:", e);
			return null;
		}
	}
}
