package pw.hshen.hrpc.codec.serialization;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
public interface Serializer {
	<T> byte[] serialize(T obj);

	<T> T deserialize(byte[] data, Class<T> cls);
}
