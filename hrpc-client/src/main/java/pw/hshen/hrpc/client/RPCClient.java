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
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Slf4j
public class RPCClient extends SimpleChannelInboundHandler<RPCResponse> {

    private final String host;
    private final int port;

    private RPCResponse response;

    public RPCClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

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
            // 创建并初始化 Netty 客户端 Bootstrap 对象
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
            // 连接 RPC 服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 写入 RPC 请求数据并关闭连接
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            // 返回 RPC 响应对象
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }
}
