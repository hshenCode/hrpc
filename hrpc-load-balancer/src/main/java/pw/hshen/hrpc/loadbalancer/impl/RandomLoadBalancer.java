package pw.hshen.hrpc.loadbalancer.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import pw.hshen.hrpc.common.model.ServiceAddress;
import pw.hshen.hrpc.loadbalancer.LoadBalancer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author hongbin
 * Created on 18/11/2017
 */
@Data
@Builder
@AllArgsConstructor
public class RandomLoadBalancer implements LoadBalancer<ServiceAddress>{

	List<ServiceAddress> serviceAddresses;

	@Override
	public ServiceAddress next() {
		return serviceAddresses.get(ThreadLocalRandom.current().nextInt(serviceAddresses.size()));
	}
}
