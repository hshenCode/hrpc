package pw.hshen.hrpc.client;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.common.model.RPCResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author hongbin
 * Created on 11/11/2017
 */
@Slf4j
@Data
@RequiredArgsConstructor
public class RPCResponseFuture implements Future<Object> {
	@NonNull
	private String requestId;

	private RPCResponse response;

	CountDownLatch latch = new CountDownLatch(1);

	public void done(RPCResponse response) {
		this.response = response;
		latch.countDown();
	}

	@Override
	public RPCResponse get() throws InterruptedException, ExecutionException {
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}
		return response;
	}

	@Override
	public RPCResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		try {
			if (!latch.await(timeout, unit)) {
				throw new TimeoutException("RPC Request timeout!");
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}
		return response;
	}

	@Override
	public boolean isDone() {
		return latch.getCount() == 0;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}
}
