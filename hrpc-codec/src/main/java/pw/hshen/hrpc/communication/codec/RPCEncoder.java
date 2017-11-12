package pw.hshen.hrpc.communication.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import pw.hshen.hrpc.communication.serialization.Serializer;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@AllArgsConstructor
public class RPCEncoder extends MessageToByteEncoder {

	private Class<?> genericClass;
	private Serializer serializer;

	@Override
	public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
		if (genericClass.isInstance(in)) {
			byte[] data = serializer.serialize(in);
			out.writeInt(data.length);
			out.writeBytes(data);
		}
	}
}
