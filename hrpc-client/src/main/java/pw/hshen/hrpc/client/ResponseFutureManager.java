package pw.hshen.hrpc.client;

import pw.hshen.hrpc.common.model.RPCResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hongbin
 * Created on 11/11/2017
 */
public class ResponseFutureManager {
	/**
	 * Singleton
	 */
	private static ResponseFutureManager rpcFutureManager;

	private ResponseFutureManager(){}

	public static ResponseFutureManager getInstance() {
		if (rpcFutureManager == null) {
			synchronized (ChannelManager.class) {
				if (rpcFutureManager == null) {
					rpcFutureManager = new ResponseFutureManager();
				}
			}
		}
		return rpcFutureManager;
	}

	private ConcurrentHashMap<String, RPCResponseFuture> rpcFutureMap = new ConcurrentHashMap<>();

	public void registerFuture(RPCResponseFuture rpcResponseFuture) {
		rpcFutureMap.put(rpcResponseFuture.getRequestId(), rpcResponseFuture);
	}

	public void futureDone(RPCResponse response) {
		rpcFutureMap.remove(response.getRequestId()).done(response);
	}
}
