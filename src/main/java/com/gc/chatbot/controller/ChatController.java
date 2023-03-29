package com.gc.chatbot.controller;


import com.gc.chatbot.service.OpenAiChatBiz;
import com.gc.chatbot.service.EdgeChatBot;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
@Api(tags = "chat-bot")
public class ChatController {

    @Autowired
    private OpenAiChatBiz openAiChatBiz;
    @Autowired
    private EdgeChatBot chatBot;


    @PostMapping("/test")
    @ApiOperation("chat-gpt输入文字")
    public String checkSignature(String text) {
        String hd = openAiChatBiz.chat(text);
        log.info("\n组装回复信息：{}", hd.toString());
        return hd;
    }
    @PostMapping("/ack")
    @ApiOperation("edge-gpt输入文字")
    public String checkSignature1(String text) {
        CompletableFuture<String> result = chatBot.ask(text);
        String hd = null;
        try {
            hd = result.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        log.info("\n组装回复信息：{}", hd.toString());
        return hd;
    }
}