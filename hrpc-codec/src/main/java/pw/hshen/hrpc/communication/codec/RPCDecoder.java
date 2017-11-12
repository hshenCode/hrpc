package pw.hshen.hrpc.communication.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import pw.hshen.hrpc.communication.serialization.Serializer;

import java.util.List;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@AllArgsConstructor
public class RPCDecoder extends ByteToMessageDecoder {

	private Class<?> genericClass;
	private Serializer serializer;

	@Override
	public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() < 4) {
			return;
		}
		in.markReaderIndex();
		int dataLength = in.readInt();
		if (in.readableBytes() < dataLength) {
			in.resetReaderIndex();
			return;
		}
		byte[] data = new byte[dataLength];
		in.readBytes(data);
		out.add(serializer.deserialize(data, genericClass));
	}
}
