package pw.hshen.hrpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import pw.hshen.hrpc.codec.coder.RPCEncoder;
import pw.hshen.hrpc.common.model.RPCRequest;
import pw.hshen.hrpc.common.model.RPCResponse;
import pw.hshen.hrpc.codec.serialization.impl.ProtobufSerializer;
import pw.hshen.hrpc.registry.ServiceRegistry;
import pw.hshen.hrpc.common.annotation.RPCService;
import pw.hshen.hrpc.codec.coder.RPCDecoder;
import pw.hshen.hrpc.server.handler.RPCServerHandler;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@RequiredArgsConstructor
@Slf4j
public class RPCServer implements ApplicationContextAware, InitializingBean {

    @NonNull
    private String serviceAddress;

	@NonNull
	private ServiceRegistry serviceRegistry;

	private Map<String, Object> handlerMap = new HashMap<>();

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		log.info("Putting handler");
		// Register handler
		getServiceInterfaces(ctx)
				.stream()
				.forEach(interfaceClazz -> {
					String serviceName = interfaceClazz.getAnnotation(RPCService.class).value().getName();
					Object serviceBean = ctx.getBean(interfaceClazz);
					handlerMap.put(serviceName, serviceBean);
					log.debug("Put handler: {}, {}", serviceName, serviceBean);
				});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		startServer();
	}

	private void startServer() {
		// Get ip and port
		String[] addressArray = StringUtils.split(serviceAddress, ":");
		String ip = addressArray[0];
		int port = Integer.parseInt(addressArray[1]);

		log.debug("Starting server on port: {}", port);
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel) throws Exception {
							ChannelPipeline pipeline = channel.pipeline();
							pipeline.addLast(new RPCDecoder(RPCRequest.class, new ProtobufSerializer()));
							pipeline.addLast(new RPCEncoder(RPCResponse.class, new ProtobufSerializer()));
							pipeline.addLast(new RPCServerHandler(handlerMap));
						}
					});
			bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
			bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

			ChannelFuture future = bootstrap.bind(ip, port).sync();
			log.info("Server started");

			registerServices();

			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			throw new RuntimeException("Server shutdown!", e);
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	private void registerServices() {
		if (serviceRegistry != null && serviceAddress != null) {
			for (String interfaceName : handlerMap.keySet()) {
				serviceRegistry.register(interfaceName, serviceAddress.toString());
				log.info("Registering service: {} with address: {}", interfaceName, serviceAddress);
			}
		}
	}

	private List<Class<?>> getServiceInterfaces(ApplicationContext ctx) {
		Class<? extends Annotation> clazz = RPCService.class;
		return ctx.getBeansWithAnnotation(clazz)
				.values().stream()
				.map(AopUtils::getTargetClass)
				.map(cls -> Arrays.asList(cls.getInterfaces()))
				.flatMap(List::stream)
				.filter(cls -> Objects.nonNull(cls.getAnnotation(clazz)))
				.collect(Collectors.toList());
	}
}
