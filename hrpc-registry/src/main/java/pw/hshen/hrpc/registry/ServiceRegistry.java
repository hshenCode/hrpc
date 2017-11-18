package pw.hshen.hrpc.registry;

import pw.hshen.hrpc.common.model.ServiceAddress;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
public interface ServiceRegistry {
	void register(String serviceName, ServiceAddress serviceAddress);
}