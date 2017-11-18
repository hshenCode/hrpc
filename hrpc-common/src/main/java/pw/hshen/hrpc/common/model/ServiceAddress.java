package pw.hshen.hrpc.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hongbin
 * Created on 18/11/2017
 */
@Data
@AllArgsConstructor
public class ServiceAddress {
	private String ip;
	private int port;

	@Override
	public String toString(){
		return ip + ":" + port;
	}
}
