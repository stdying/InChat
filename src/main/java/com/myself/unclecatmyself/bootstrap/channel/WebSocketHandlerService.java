package com.myself.unclecatmyself.bootstrap.channel;

import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import com.myself.unclecatmyself.bootstrap.backmsg.InChatBackMapService;
import com.myself.unclecatmyself.bootstrap.BaseAuthService;
import com.myself.unclecatmyself.bootstrap.WsChannelService;
import com.myself.unclecatmyself.common.websockets.ServerWebSocketHandlerService;
import com.myself.unclecatmyself.bootstrap.verify.InChatVerifyService;
import com.myself.unclecatmyself.task.DataAsynchronousTask;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by MySelf on 2018/11/21.
 */
@Slf4j
@Component
public class WebSocketHandlerService extends ServerWebSocketHandlerService{

    @Autowired
    InChatVerifyService inChatVerifyService;

    @Autowired
    InChatBackMapService inChatBackMapService;

    @Autowired
    WsChannelService websocketChannelService;

    @Autowired
    DataAsynchronousTask dataAsynchronousTask;

    private final Gson gson;

    private final BaseAuthService baseAuthService;

    public WebSocketHandlerService(BaseAuthService baseAuthService,Gson gson){
        this.baseAuthService = baseAuthService;
        this.gson = gson;
    }

    @Override
    public boolean login(Channel channel, Map<String,Object> maps) {
        //校验规则，自定义校验规则
        String token = (String) maps.get("token");
        if (inChatVerifyService.verifyToken(token)){
            channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(inChatBackMapService.loginSuccess())));
            websocketChannelService.loginWsSuccess(channel,token);
            return true;
        }
        channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(inChatBackMapService.loginError())));
        close(channel);
        return false;
    }

    @Override
    public void sendMeText(Channel channel, Map<String,Object> maps) {
        channel.writeAndFlush(new TextWebSocketFrame(
                gson.toJson(inChatBackMapService.sendMe((String) maps.get("value")))));
        try {
            dataAsynchronousTask.writeData(maps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendToText(Channel channel, Map<String, Object> maps) {
        String otherOne = (String) maps.get("one");
        String value = (String) maps.get("value");
        String me = (String) maps.get("me");
        if (websocketChannelService.hasOther(otherOne)){
            //发送给对方
            Channel other = websocketChannelService.getChannel(otherOne);
            other.writeAndFlush(new TextWebSocketFrame(
                    gson.toJson(inChatBackMapService.getMsg(me,value))));
            //返回给自己
            channel.writeAndFlush(new TextWebSocketFrame(
                    gson.toJson(inChatBackMapService.sendBack(otherOne,value))));
        }
        try {
            dataAsynchronousTask.writeData(maps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void pong(Channel channel) {
        log.info("【pong】"+channel.remoteAddress());

    }

    @Override
    public void disconnect(Channel channel) {
        log.info("【disconnect】"+channel.remoteAddress());

    }

    @Override
    public void doTimeOut(Channel channel, IdleStateEvent evt) {
        log.info("【PingPongService：doTimeOut 心跳超时】" + channel.remoteAddress() + "【channel 关闭】");

    }

    @Override
    public void sendGroupText(Channel channel, Map<String, Object> maps) {
        String groupId = (String) maps.get("groupId");
        String me = (String) maps.get("me");
        String value = (String) maps.get("value");
        JSONArray array = inChatVerifyService.getArrayByGroupId(groupId);
        for (Object item:array) {
            if (websocketChannelService.hasOther((String) item)){
                Channel other = websocketChannelService.getChannel((String) item);
                other.writeAndFlush(new TextWebSocketFrame(
                        gson.toJson(inChatBackMapService.sendGroup(me,value,groupId))));
            }
        }
        try {
            dataAsynchronousTask.writeData(maps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(Channel channel) {
        log.info("【close】"+channel.remoteAddress());
        websocketChannelService.close(channel);
    }
}
