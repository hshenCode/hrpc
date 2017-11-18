package pw.hshen.hrpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.common.model.RPCRequest;
import pw.hshen.hrpc.common.model.RPCResponse;
import pw.hshen.hrpc.codec.coder.RPCDecoder;
import pw.hshen.hrpc.codec.coder.RPCEncoder;
import pw.hshen.hrpc.codec.serialization.impl.ProtobufSerializer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage the lifecycle of channels
 *
 * @author hongbin
 * Created on 09/11/2017
 */
@Slf4j
public class ChannelManager {
	/**
	 * Singleton
	 */
	private static ChannelManager channelManager;

	private ChannelManager(){}

	public static ChannelManager getInstance() {
		if (channelManager == null) {
			synchronized (ChannelManager.class) {
				if (channelManager == null) {
					channelManager = new ChannelManager();
				}
			}
		}
		return channelManager;
	}

	private Map<InetSocketAddress, Channel> channels = new ConcurrentHashMap<>();

	public Channel getChannel(InetSocketAddress inetSocketAddress) {
		Channel channel = channels.get(inetSocketAddress);
		if (null == channel) {
			EventLoopGroup group = new NioEventLoopGroup();
			try {
				Bootstrap bootstrap = new Bootstrap();
				bootstrap.group(group)
						.channel(NioSocketChannel.class)
						.handler(new RPCChannelInitializer())
						.option(ChannelOption.SO_KEEPALIVE, true);

				channel = bootstrap.connect(inetSocketAddress.getHostName(), inetSocketAddress.getPort()).sync()
						.channel();
				registerChannel(inetSocketAddress, channel);

				channel.closeFuture().addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						removeChannel(inetSocketAddress);
					}
				});
			} catch (Exception e) {
				log.warn("Fail to get channel for address: {}", inetSocketAddress);
			}
		}
		return channel;
	}

	private void registerChannel(InetSocketAddress inetSocketAddress, Channel channel) {
		channels.put(inetSocketAddress, channel);
	}

	private void removeChannel(InetSocketAddress inetSocketAddress) {
		channels.remove(inetSocketAddress);
	}

	private class RPCChannelInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new RPCEncoder(RPCRequest.class, new ProtobufSerializer()));
			pipeline.addLast(new RPCDecoder(RPCResponse.class, new ProtobufSerializer()));
			pipeline.addLast(new RPCResponseHandler());
		}
	}

	private class RPCResponseHandler extends SimpleChannelInboundHandler<RPCResponse> {

		@Override
		public void channelRead0(ChannelHandlerContext ctx, RPCResponse response) throws Exception {
			ResponseFutureManager.getInstance().futureDone(response);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			log.warn("RPC request exception: {}", cause);
		}
	}
}
