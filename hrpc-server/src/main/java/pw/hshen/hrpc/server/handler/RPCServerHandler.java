package pw.hshen.hrpc.server.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.common.model.RPCRequest;
import pw.hshen.hrpc.common.model.RPCResponse;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Slf4j
public class RPCServerHandler extends SimpleChannelInboundHandler<RPCRequest> {

	private final Map<String, Object> handlerMap;

	public RPCServerHandler(Map<String, Object> handlerMap) {
		this.handlerMap = handlerMap;
	}

	@Override
	public void channelRead0(final ChannelHandlerContext ctx, RPCRequest request) throws Exception {
		// 创建并初始化 RPC 响应对象
		RPCResponse response = new RPCResponse();
		response.setRequestId(request.getRequestId());
		try {
			Object result = handle(request);
			response.setResult(result);
		} catch (Exception e) {
			log.warn("handle result failure");
			response.setException(e);
		}
		// 写入 RPC 响应对象并自动关闭连接
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private Object handle(RPCRequest request) throws Exception {
		// 获取服务对象
		String serviceName = request.getInterfaceName();
		Object serviceBean = handlerMap.get(serviceName);
		if (serviceBean == null) {
			throw new RuntimeException(String.format("can not find service bean by key: %s", serviceName));
		}
		// 获取反射调用所需的参数
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
