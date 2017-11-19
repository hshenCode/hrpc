> 在后续一段时间里， 我会写一系列文章来讲述如何实现一个RPC框架。 这是系列第三篇文章， 主要讲述了服务注册和服务发现这一块。

在系列的第一篇文章中提到，我们的RPC框架需要有一个服务注册中心。 通过这个中心，服务可以把自己的信息注册进来，也可以获取到别的服务的信息（例如ip、端口、版本信息等）。这一块有个统一的名称，叫服务发现。

对于服务发现，现在有很多可供选择的工具，例如zookeeper, etcd或者是consul等。 有一篇文章专门对这三个工具做了对比： [服务发现：Zookeeper vs etcd vs Consul](http://dockone.io/article/667)。 在我的框架中， 我选择使用Consul来实现服务发现。对于Consul不了解的朋友可以去看我之前写的[关于Consul的博客](http://blog.csdn.net/u012422829/article/details/77803799)。

Consul客户端也有一些Java的实现，我用到了[consul-api](https://github.com/Ecwid/consul-api)。

## 服务注册
首先，我们定义一个接口：

```
public interface ServiceRegistry {
    void register(String serviceName, ServiceAddress serviceAddress);
}

```
这个接口很简单，向服务注册中心注册自己的地址。

对应的consul的实现：

```
public class ConsulServiceRegistry implements ServiceRegistry {

	private ConsulClient consulClient;

	public ConsulServiceRegistry(String consulAddress) {
		String address[] = consulAddress.split(":");
		ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
		consulClient = new ConsulClient(rawClient);
	}

	@Override
	public void register(String serviceName, ServiceAddress serviceAddress) {
		NewService newService = new NewService();
		newService.setId(generateNewIdForService(serviceName, serviceAddress));
		newService.setName(serviceName);
		newService.setTags(new ArrayList<>());
		newService.setAddress(serviceAddress.getIp());
		newService.setPort(serviceAddress.getPort());
		
		// Set health check
		NewService.Check check = new NewService.Check();
		check.setTcp(serviceAddress.toString());
		check.setInterval("1s");
		newService.setCheck(check);
		
		consulClient.agentServiceRegister(newService);
	}

	private String generateNewIdForService(String serviceName, ServiceAddress serviceAddress){
		// serviceName + ip + port
		return serviceName + "-" + serviceAddress.getIp() + "-" + serviceAddress.getPort();
	}
}
```
这里我向consul注册服务的时候，还设定了健康状态检查方式为TCP连接方式， 即每过一秒，consul都会尝试与该地址建立TCP连接以验证服务状态。 除了TCP连接之外，consul还提供了http、ttl等多种检查方式。

另外一点值得注意的是，要确保id绝对唯一。 我能想到的比较直观的解决方案是serviceName + 本机ip + 本机port的组合。


## 服务发现
 对于服务发现而言， 值得注意的是，我们需要去watch consul上值的变化， 并更新保存在应用中的服务的地址。
 
首先，我们定义一个接口：

```
public interface ServiceDiscovery {
    String discover(String serviceName);
}
```
这个接口很简单，传入serviceName，获取一个可以访问的该service的地址。


对应的consul的实现：

```
public class ConsulServiceDiscovery implements ServiceDiscovery {

	private ConsulClient consulClient;

	// 这里我用到了LoadBalancer, 关于LB这块，后续文章会专门讲述
	Map<String, LoadBalancer<ServiceAddress>> loadBalancerMap = new ConcurrentHashMap<>();

	public ConsulServiceDiscovery(String consulAddress) {
		String[] address = consulAddress.split(":");
		ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
		consulClient = new ConsulClient(rawClient);
	}

	@Override
	public String discover(String serviceName) {
		List<HealthService> healthServices;
		if (!loadBalancerMap.containsKey(serviceName)) {
			healthServices = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT)
					.getValue();
			loadBalancerMap.put(serviceName, buildLoadBalancer(healthServices));

			// Watch consul
			longPolling(serviceName);
		}
		return loadBalancerMap.get(serviceName).next().toString();
	}

	private void longPolling(String serviceName){
		new Thread(new Runnable() {
			@Override
			public void run() {
				long consulIndex = -1;
				do {

					QueryParams param =
							QueryParams.Builder.builder()
									.setIndex(consulIndex)
									.build();

					Response<List<HealthService>> healthyServices =
							consulClient.getHealthServices(serviceName, true, param);

					consulIndex = healthyServices.getConsulIndex();
					log.debug("consul index for {} is: {}", serviceName, consulIndex);

					List<HealthService> healthServices = healthyServices.getValue();
					log.debug("service addresses of {} is: {}", serviceName, healthServices);

					loadBalancerMap.put(serviceName, buildLoadBalancer(healthServices));
				} while(true);
			}
		}).start();
	}

	private LoadBalancer buildLoadBalancer(List<HealthService> healthServices) {
		return new RandomLoadBalancer(healthServices.stream()
				.map(healthService -> {
					HealthService.Service service =healthService.getService();
					return new ServiceAddress(service.getAddress() , service.getPort());
				})
				.collect(Collectors.toList()));
	}
}

```
这里我用到了LoadBalancer, 关于LB这块，后续文章会专门讲述。 除此之外， 这里的核心是在获取完服务地址之后，会watch该服务地址的变化， 并更新对应的LB中的地址列表。

就这样， 一个简单的服务注册与发现功能就实现了。 完整代码请看[我的github](https://github.com/hshenCode/hrpc)。
