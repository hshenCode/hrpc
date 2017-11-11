package pw.hshen.hrpc.registry.impl.zookeeper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.util.CollectionUtils;
import pw.hshen.hrpc.registry.ServiceDiscovery;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Slf4j
public class ZKServiceDiscovery implements ServiceDiscovery {

	private String zkAddress;

	public ZKServiceDiscovery(String zkAddress) {
		log.debug("set zkAddress: {}", zkAddress);
		this.zkAddress = zkAddress;
	}

	@Override
	public String discover(String name) {
		log.debug("connecting to zk: {}", zkAddress);
		ZkClient zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
		log.debug("zookeeper connected");
		try {
			// 获取 service 节点
			String servicePath = Constant.ZK_REGISTRY_PATH + "/" + name;
			if (!zkClient.exists(servicePath)) {
				throw new RuntimeException(String.format("can not find any service node on path: %s", servicePath));
			}
			List<String> addressList = zkClient.getChildren(servicePath);
			if (CollectionUtils.isEmpty(addressList)) {
				throw new RuntimeException(String.format("can not find any address node on path: %s", servicePath));
			}
			// 获取 address 节点
			String address;
			int size = addressList.size();
			if (size == 1) {
				// 若只有一个地址，则获取该地址
				address = addressList.get(0);
				log.debug("get only address node: {}", address);
			} else {
				// 若存在多个地址，则随机获取一个地址
				address = addressList.get(ThreadLocalRandom.current().nextInt(size));
				log.debug("get random address node: {}", address);
			}
			// 获取 address 节点的值
			String addressPath = servicePath + "/" + address;
			return zkClient.readData(addressPath);
		} finally {
			zkClient.close();
		}
	}
}
