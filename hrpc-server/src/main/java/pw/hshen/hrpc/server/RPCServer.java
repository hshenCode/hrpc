package pw.hshen.hrpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import pw.hshen.hrpc.communication.codec.RPCEncoder;
import pw.hshen.hrpc.communication.model.RPCRequest;
import pw.hshen.hrpc.communication.model.RPCResponse;
import pw.hshen.hrpc.communication.serialization.impl.ProtobufSerializer;
import pw.hshen.hrpc.registry.ServiceRegistry;
import pw.hshen.hrpc.server.annotation.RPCService;
import pw.hshen.hrpc.communication.codec.RPCDecoder;
import pw.hshen.hrpc.server.handler.RPCServerHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hongbin
 * Created on 21/10/2017
 */
@AllArgsConstructor
@Slf4j
public class RPCServer implements ApplicationContextAware, InitializingBean {

    private String serviceAddress;

    public RPCServer(String serviceAddress, ServiceRegistry serviceRegistry) {
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    private ServiceRegistry serviceRegistry;

    /**
     * 存放 服务名 与 服务对象 之间的映射关系
     */
    private Map<String, Object> handlerMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        log.debug("Start put handler");
        // 扫描带有 RPCService 注解的类并初始化 handlerMap 对象
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RPCService.class);
        if (!CollectionUtils.isEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RPCService RPCService = serviceBean.getClass().getAnnotation(RPCService.class);
                String serviceName = RPCService.value().getName();
                handlerMap.put(serviceName, serviceBean);
                log.debug("put handler: {}, {}", serviceName, serviceBean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 创建并初始化 Netty 服务端 Bootstrap 对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RPCDecoder(RPCRequest.class, new ProtobufSerializer()));
                    pipeline.addLast(new RPCEncoder(RPCResponse.class, new ProtobufSerializer()));
                    pipeline.addLast(new RPCServerHandler(handlerMap));
                }
            });
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 获取 RPC 服务器的 IP 地址与端口号
            String[] addressArray = StringUtils.split(serviceAddress, ":");
            String ip = addressArray[0];
            int port = Integer.parseInt(addressArray[1]);
            // 启动 RPC 服务器
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            // 注册 RPC 服务地址
            if (serviceRegistry != null) {
                for (String interfaceName : handlerMap.keySet()) {
                    serviceRegistry.register(interfaceName, serviceAddress);
                    log.debug("register service: {} => {}", interfaceName, serviceAddress);
                }
            }
            log.debug("server started on port {}", port);
            // 关闭 RPC 服务器
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
