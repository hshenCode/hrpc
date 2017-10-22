package pw.hshen.hrpc.communication.model;

import lombok.Data;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Data
public class RPCRequest {
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
