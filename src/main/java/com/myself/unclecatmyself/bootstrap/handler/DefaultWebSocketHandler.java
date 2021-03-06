package com.myself.unclecatmyself.bootstrap.handler;

import com.alibaba.fastjson.JSON;
import com.myself.unclecatmyself.common.exception.NoFindHandlerException;
import com.myself.unclecatmyself.common.websockets.ServerWebSocketHandlerService;
import com.myself.unclecatmyself.common.websockets.WebSocketHandler;
import com.myself.unclecatmyself.common.websockets.WebSocketHandlerApi;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 20:15 2018\11\16 0016
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class DefaultWebSocketHandler extends WebSocketHandler {

    private final WebSocketHandlerApi webSocketHandlerApi;

    public DefaultWebSocketHandler(WebSocketHandlerApi webSocketHandlerApi) {
        super(webSocketHandlerApi);
        this.webSocketHandlerApi = webSocketHandlerApi;
    }

    @Override
    protected void webdoMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {

    }

    @Override
    protected void textdoMessage(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        Channel channel = ctx.channel();
        ServerWebSocketHandlerService serverWebSocketHandlerService;
        if (webSocketHandlerApi instanceof ServerWebSocketHandlerService){
            serverWebSocketHandlerService = (ServerWebSocketHandlerService)webSocketHandlerApi;
        }else{
            throw new NoFindHandlerException("Server Handler 不匹配");
        }
        Map<String,Object> maps = (Map) JSON.parse(msg.text());
        switch ((String)maps.get("type")){
            case "login":
                log.info("【用户链接登录操作】");
                serverWebSocketHandlerService.login(channel,maps);
                break;
            //针对个人，发送给自己
            case "sendMe":
                log.info("【用户链接发送给自己】");
                serverWebSocketHandlerService.sendMeText(channel,maps);
                break;
            //针对个人，发送给某人
            case "sendTo":
                log.info("【用户链接发送给某人】");
                serverWebSocketHandlerService.sendToText(channel,maps);
                break;
            //发送给群组
            case "sendGroup":
                log.info("【用户链接发送给群聊】");
                serverWebSocketHandlerService.sendGroupText(channel,maps);
                break;
            default:
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("【DefaultWebSocketHandler：channelActive】"+ctx.channel().remoteAddress().toString()+"链接成功");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
        log.error("exception",cause);
        webSocketHandlerApi.close(ctx.channel());
    }
}
