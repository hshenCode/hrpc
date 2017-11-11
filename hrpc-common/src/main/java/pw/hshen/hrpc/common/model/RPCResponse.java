package pw.hshen.hrpc.common.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Data
public class RPCResponse {

    private String requestId;
    private Exception exception;
    private Object result;

    public boolean hasException() {
        return exception != null;
    }
}
