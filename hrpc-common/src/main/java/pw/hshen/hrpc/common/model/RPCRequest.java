package pw.hshen.hrpc.common.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Data
@Builder
public class RPCRequest {
	private String requestId;
	private String interfaceName;
	private String methodName;
	private Class<?>[] parameterTypes;
	private Object[] parameters;
}
