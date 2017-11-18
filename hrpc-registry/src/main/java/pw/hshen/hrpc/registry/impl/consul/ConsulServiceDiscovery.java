package pw.hshen.hrpc.registry.impl.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.registry.ServiceDiscovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Slf4j
public class ConsulServiceDiscovery implements ServiceDiscovery {

	private ConsulClient consulClient;

	Map<String, List<HealthService>> healthServicesMap = new ConcurrentHashMap<>();

	public ConsulServiceDiscovery(String consulAddress) {
		String[] address = consulAddress.split(":");
		ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
		consulClient = new ConsulClient(rawClient);
	}

	@Override
	public String discover(String serviceName) {
		List<HealthService> healthServices;
		if (healthServicesMap.containsKey(serviceName)) {
			healthServices = healthServicesMap.get(serviceName);
		} else {
			healthServices = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT)
					.getValue();
			healthServicesMap.put(serviceName, healthServices);
			longPolling(serviceName);
		}
		// TODO: Just return random now. We'll introduce load balancer later.
		HealthService.Service service =null;
		try {
			service = healthServices.get(ThreadLocalRandom.current().nextInt(healthServices.size())).getService();
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
		return service.getAddress() + ":" + service.getPort();
	}

	// TODO: new thread.
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

					List<HealthService> services = healthyServices.getValue();
					log.debug("service addresses of {} is: {}", serviceName, services);

					healthServicesMap.put(serviceName, services);
				} while(true);
			}
		}).start();
	}
}
