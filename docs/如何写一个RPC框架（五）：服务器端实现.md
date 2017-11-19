> 在后续一段时间里， 我会写一系列文章来讲述如何实现一个RPC框架。 这是系列第五篇文章， 主要讲述了服务器端的实现。

在前面的几篇文章里， 我们已经实现了客户端创建proxy bean， 并利用它来发送请求、处理返回的全部流程： 
1. 扫描package找出需要代理的service
2. 通过服务注册中心和Load Balancer获取service地址
3. 利用Netty与service建立连接， 并且复用所创建的channel
4. 创建request， 用唯一的requestId来标识它， 发送这个请求， 调用future.get()
4. 收到response，利用response中附带的requestId找到对应future，让future变成done的状态

这篇文章， 我们会介绍server端的实现。

## 1.获取server端所实现的接口

```
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
```
利用Spring的ApplicationContext， 获取bean容器中带有RPCService注解的bean。

## 2.bean与对应的接口名一一对应并保存在map中
```
private Map<String, Object> handlerMap = new HashMap<>();
getServiceInterfaces(ctx)
				.stream()
				.forEach(interfaceClazz -> {
					String serviceName = interfaceClazz.getAnnotation(RPCService.class).value().getName();
					Object serviceBean = ctx.getBean(interfaceClazz);
					handlerMap.put(serviceName, serviceBean);
					log.debug("Put handler: {}, {}", serviceName, serviceBean);
				});
```
handlerMap的作用是， 收到请求时， 可以通过这个map找到该请求所对应的处理对象。


## 3.启动服务器并注册所实现的service

```
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
```
这里几个地方需要说明一下：
1. 这里的ip和port我直接用了配置文件中传入的， 优化的方案应该是获取本地ip以及找到一个可用端口
2. 利用Netty创建server， 在pipeline中加入RPCServerHandler， 这个handler将在下文给出
3. 向服务注册中心注册实现的所有服务

## 4.RPCServerHandler的实现

```
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
```
这个实现相当简单，收到请求之后， 根据servicename，找到对应的handler，再利用反射进行方法调用。

就这样， 一个简单的RPCServer就实现了。 完整代码请看[我的github](https://github.com/hshenCode/hrpc)。