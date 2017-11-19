package pw.hshen.hrpc.server.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.common.model.RPCRequest;
import pw.hshen.hrpc.common.model.RPCResponse;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Handle the RPC request
 *
 * @author hongbin
 * Created on 21/10/2017
 */
@Slf4j
@AllArgsConstructor
public class RPCServerHandler extends SimpleChannelInboundHandler<RPCRequest> {

	private Map<String, Object> handlerMap;

	@Override
	public void channelRead0(final ChannelHandlerContext ctx, RPCRequest request) throws Exception {
		log.debug("Get request: {}", request);
		RPCResponse response = new RPCResponse();
		response.setRequestId(request.getRequestId());
		try {
			Object result = handleRequest(request);
			response.setResult(result);
		} catch (Exception e) {
			log.warn("Get exception when hanlding request, exception: {}", e);
			response.setException(e);
		}
		ctx.writeAndFlush(response).addListener(
				(ChannelFutureListener) channelFuture -> {
					log.debug("Sent response for request: {}", request.getRequestId());
				});
	}

	private Object handleRequest(RPCRequest request) throws Exception {
		// Get service bean
		String serviceName = request.getInterfaceName();
		Object serviceBean = handlerMap.get(serviceName);
		if (serviceBean == null) {
			throw new RuntimeException(String.format("No service bean available: %s", serviceName));
		}

		// Invoke by reflect
		Class<?> serviceClass = serviceBean.getClass();
		String methodName = request.getMethodName();
		Class<?>[] parameterTypes = request.getParameterTypes();
		Object[] parameters = request.getParameters();
		Method method = serviceClass.getMethod(methodName, parameterTypes);
		method.setAccessible(true);
		return method.invoke(serviceBean, parameters);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("server caught exception", cause);
		ctx.close();
	}
}
