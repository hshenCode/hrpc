package pw.hshen.hrpc.registry.impl.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.health.model.HealthService;
import pw.hshen.hrpc.registry.ServiceDiscovery;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
public class ConsulServiceDiscovery implements ServiceDiscovery {

	private ConsulClient consulClient;

	public ConsulServiceDiscovery(String consulAddress) {
		String[] address = consulAddress.split(":");
		ConsulRawClient rawClient = new ConsulRawClient(address[0], Integer.valueOf(address[1]));
		consulClient = new ConsulClient(rawClient);
	}

	@Override
	public String discover(String serviceName) {
		List<HealthService> healthServices = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT).getValue();
		// TODO: Just return random now. We'll introduce load balance later.
		return healthServices.get(ThreadLocalRandom.current().nextInt(healthServices.size())).getService().getAddress();
	}
}
