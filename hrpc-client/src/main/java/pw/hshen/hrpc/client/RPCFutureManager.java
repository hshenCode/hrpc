package pw.hshen.hrpc.client;

import pw.hshen.hrpc.common.model.RPCResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hongbin
 * Created on 11/11/2017
 */
public class RPCFutureManager {
	/**
	 * Singleton
	 */
	private static RPCFutureManager rpcFutureManager;

	private RPCFutureManager(){}

	public static RPCFutureManager getInstance() {
		if (rpcFutureManager == null) {
			synchronized (ChannelManager.class) {
				if (rpcFutureManager == null) {
					rpcFutureManager = new RPCFutureManager();
				}
			}
		}
		return rpcFutureManager;
	}

	private ConcurrentHashMap<String, RPCFuture> rpcFutureMap = new ConcurrentHashMap<>();

	public void registerFuture(String requestId, RPCFuture rpcFuture) {
		rpcFutureMap.put(requestId, rpcFuture);
	}

	public void futureDone(RPCResponse response) {
		rpcFutureMap.remove(response.getRequestId()).done(response);
	}
}
