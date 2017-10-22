package pw.hshen.hrpc.registry;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
public interface ServiceRegistry {
    void register(String serviceName, String serviceAddress);
}
