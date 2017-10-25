package pw.hshen.hrpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pw.hshen.hrpc.communication.codec.RPCDecoder;
import pw.hshen.hrpc.communication.codec.RPCEncoder;
import pw.hshen.hrpc.communication.model.RPCRequest;
import pw.hshen.hrpc.communication.model.RPCResponse;
import pw.hshen.hrpc.communication.serialization.impl.ProtobufSerializer;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@Slf4j
@RequiredArgsConstructor
public class RPCClient extends SimpleChannelInboundHandler<RPCResponse> {

    private final String host;
    private final int port;

    private RPCResponse response;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RPCResponse response) throws Exception {
        this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("api caught exception: {}", cause);
        ctx.close();
    }

    public RPCResponse send(RPCRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RPCEncoder(RPCRequest.class, new ProtobufSerializer()));
                    pipeline.addLast(new RPCDecoder(RPCResponse.class, new ProtobufSerializer()));
                    pipeline.addLast(RPCClient.this);
                }
            });
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            ChannelFuture future = bootstrap.connect(host, port).sync();
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }
}
