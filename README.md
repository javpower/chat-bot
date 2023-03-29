# ChatBot

#### 介绍
springboot 集成openAi的ChatGpt,微软的新必应EdgeGpt,并附带聊天界面

#### 使用说明

1.  启动spring boot项目
2.  打开浏览器使用
![ChatGpt](https://foruda.gitee.com/images/1680077317156046535/da56ad63_2334850.png "屏幕截图")
![EdgeGpt](https://foruda.gitee.com/images/1680077351096423672/ba61c376_2334850.png "屏幕截图")

#### 申明
请不要使用代码中的密钥和cookie，换成自己的
![openAi密钥和模型配置](https://foruda.gitee.com/images/1680077493257245080/0caebeb6_2334850.png "屏幕截图")
![cookies和模型配置](https://foruda.gitee.com/images/1680077553571875337/bb418e98_2334850.png "屏幕截图")
"h3precise" -- 准确模式 "h3imaginative" -- 创造模式 "harmonyv3" -- 均衡模式

#### 软件架构

1、集成微软的新必应EdgeGpt
代码参考了acheong08/EdgeGPT这个仓库

-- 第一步微软账号登录拿到cookie(_U字段)
![输入图片说明](https://foruda.gitee.com/images/1680076388423235824/44e6a37f_2334850.png "屏幕截图")

-- 第二步带上自己的cookie(_U字段)去下面这几个网站中的任意一个请求一个conversation

"https://www.bing.com/turing/conversation/create"
"https://cn.bing.com/turing/conversation/create"
"https://edgeservices.bing.com/edgesvc/turing/conversation/create"

数据格式如下：
![输入图片说明](https://foruda.gitee.com/images/1680076496311868517/531e3955_2334850.png "屏幕截图")

-- 第三步带着这个conversation去和"wss://sydney.bing.com/sydney/ChatHub"建立一个websocket就可以通过websocket和edgegpt交流了。

代码片段

```
package com.gc.chatbot.service;

import lombok.Data;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@Data
public class EdgeChatBot {
    private static final String COOKIE_U = "_U=" + "YOUR COOKIE";

    private static final String DELIMITER = "\u001E";

    private static int invocationId = 0;
    private String mode="h3precise";
    private ChatHub chatHub;

    @PostConstruct
    public void init() {
        try {
            create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void create() throws IOException {
        JSONObject conversation = createConversation();
        chatHub = new ChatHub(conversation, mode);
    }

    public CompletableFuture<String> ask(String prompt) {
        CompletableFuture<String> result = new CompletableFuture<>();
        chatHub.ask(prompt, result::complete);
        return result;
    }
    private static String generateRandomIP() {
        Random random = new Random();
        int a = random.nextInt(4) + 104;
        int b = random.nextInt(256);
        int c = random.nextInt(256);
        return "13." + a + "." + b + "." + c;
    }
    private static Headers generateHeaders() {
        String forwardedIP = generateRandomIP();
        String uuid = UUID.randomUUID().toString();

        return new Headers.Builder()
                .add("accept", "application/json")
                .add("accept-language", "en-US,en;q=0.9")
                .add("content-type", "application/json")
                .add("sec-ch-ua", "\"Not_A Brand\";v=\"99\", \"Microsoft Edge\";v=\"110\", \"Chromium\";v=\"110\"")
                .add("sec-ch-ua-arch", "\"x86\"")
                .add("sec-ch-ua-bitness", "\"64\"")
                .add("sec-ch-ua-full-version", "\"109.0.1518.78\"")
                .add("sec-ch-ua-full-version-list", "\"Chromium\";v=\"110.0.5481.192\", \"Not A(Brand\";v=\"24.0.0.0\", \"Microsoft Edge\";v=\"110.0.1587.69\"")
                .add("sec-ch-ua-mobile", "?0")
                .add("sec-ch-ua-model", "\"\"")
                .add("sec-ch-ua-platform", "\"Windows\"")
                .add("sec-ch-ua-platform-version", "\"15.0.0\"")
                .add("sec-fetch-dest", "empty")
                .add("sec-fetch-mode", "cors")
                .add("sec-fetch-site", "same-origin")
                .add("x-ms-client-request-id", uuid)
                .add("x-ms-useragent", "azsdk-js-api-client-factory/1.0.0-beta.1 core-rest-pipeline/1.10.0 OS/Win32")
                .add("Referer", "https://www.bing.com/search?q=Bing+AI&showconv=1&FORM=hpcodx")
                .add("Referrer-Policy", "origin-when-cross-origin")
                .add("x-forwarded-for", forwardedIP)
                .add("Cookie",COOKIE_U)
                .build();
    }
    private static JSONObject createConversation() throws IOException {
        Headers headers = generateHeaders();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://edge.churchless.tech/edgesvc/turing/conversation/create")
                .headers(headers)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code:" + response.code());
            }
            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }

    private static class ChatHub {
        private final JSONObject conversation;
        private final String mode;
        private WebSocketClient wsClient;

        public ChatHub(JSONObject conversation, String mode) {
            this.conversation = conversation;
            this.mode = mode;
        }

        public void ask(String prompt, Consumer<String> completionHandler) {
            final String[] bot = {""};
            try {
                URI uri = new URI("wss://sydney.bing.com/sydney/ChatHub");
                Map<String, String> headersInitConver = new HashMap<>();
                headersInitConver.put("authority", "edgeservices.bing.com");
                headersInitConver.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                headersInitConver.put("accept-language", "en-US,en;q=0.9");
                headersInitConver.put("cache-control", "max-age=0");
                headersInitConver.put("sec-ch-ua", "\"Chromium\";v=\"110\", \"Not A(Brand\";v=\"24\", \"Microsoft Edge\";v=\"110\"");
                headersInitConver.put("sec-ch-ua-arch", "\"x86\"");
                headersInitConver.put("sec-ch-ua-bitness", "\"64\"");
                headersInitConver.put("sec-ch-ua-full-version", "\"110.0.1587.69\"");
                headersInitConver.put("sec-ch-ua-full-version-list", "\"Chromium\";v=\"110.0.5481.192\", \"Not A(Brand\";v=\"24.0.0.0\", \"Microsoft Edge\";v=\"110.0.1587.69\"");
                headersInitConver.put("sec-ch-ua-mobile", "?0");
                headersInitConver.put("sec-ch-ua-model", "\"\"");
                headersInitConver.put("sec-ch-ua-platform", "\"Windows\"");
                headersInitConver.put("sec-ch-ua-platform-version", "\"15.0.0\"");
                headersInitConver.put("sec-fetch-dest", "document");
                headersInitConver.put("sec-fetch-mode", "navigate");
                headersInitConver.put("sec-fetch-site", "none");
                headersInitConver.put("sec-fetch-user", "?1");
                headersInitConver.put("upgrade-insecure-requests", "1");
                headersInitConver.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36 Edg/110.0.1587.69");
                headersInitConver.put("x-edge-shopping-flag", "1");
                headersInitConver.put("x-forwarded-for", "1.1.1.1");
                wsClient = new WebSocketClient(uri,headersInitConver) {
                    @Override
                    public void onOpen(ServerHandshake serverHandshake) {

                        JSONObject json = new JSONObject();
                        json.put("protocol", "json");
                        json.put("version", 1);
                        send(json.toString()+ DELIMITER);
                        JSONObject chatRequest = createChatRequest(prompt);
                        String message = chatRequest.toString() + DELIMITER;
                        System.out.println("===========\n"+message);
                        send(message);

                    }

                    @Override
                    public void onMessage(String message) {
                        String[] messages = message.split(DELIMITER);
                        for (String msg : messages) {
                            if (msg.isEmpty()||msg.equals("{}")) {
                                continue;
                            }
                            JSONObject response = new JSONObject(msg);
                            if (response.getInt("type") == 1){
                                JSONObject responseObject = response.getJSONArray("arguments").getJSONObject(0);
                                if(responseObject.keySet().contains("messages")){
                                    JSONArray messagesArray = responseObject.getJSONArray("messages");
                                    JSONObject adaptiveCard = messagesArray.getJSONObject(0).getJSONArray("adaptiveCards").getJSONObject(0);
                                    String responseText = adaptiveCard.getJSONArray("body").getJSONObject(0).getString("text");
                                    bot[0] =responseText;
                                }
                            }else if (response.getInt("type") == 2){
                                  completionHandler.accept(bot[0]);
                            }
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        System.out.println("WebSocket closed: " + reason);
                    }

                    @Override
                    public void onError(Exception ex) {
                        System.err.println("WebSocket error: ");
                        ex.printStackTrace();
                    }
                };
                wsClient.connectBlocking();
            } catch (URISyntaxException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private JSONObject createChatRequest(String prompt) {
            invocationId++;
            JSONObject messageObject = new JSONObject()
                    .put("author", "user")
                    .put("inputMethod", "Keyboard")
                    .put("text", prompt)
                    .put("messageType", "Chat");

            JSONObject participantObject = new JSONObject()
                    .put("id", conversation.getString("clientId"));

            JSONArray optionsSets = new JSONArray()
                    .put("nlu_direct_response_filter")
                    .put("deepleo")
                    .put("disable_emoji_spoken_text")
                    .put("responsible_ai_policy_235")
                    .put("enablemm")
                  //  .put("enable_debug_commands")
                    .put(mode)
                    .put("dtappid")
                    .put("trn8req120")
                    .put("h3ads")
                    .put("rai251")
                    .put("blocklistv2")
                    .put("localtime")
                    .put("dv3sugg");

            JSONObject chatRequestArguments = new JSONObject()
                    .put("source", "cib")
                    .put("optionsSets", optionsSets)
                    .put("isStartOfSession", invocationId == 1)
                    .put("message", messageObject)
                    .put("conversationSignature", conversation.getString("conversationSignature"))
                    .put("participant", participantObject)
                    .put("conversationId", conversation.getString("conversationId"));

            JSONArray argumentsArray = new JSONArray().put(chatRequestArguments);

            return new JSONObject()
                    .put("arguments", argumentsArray)
                    .put("invocationId", String.valueOf(invocationId))
                    .put("target", "chat")
                    .put("type", 4);
        }
    }
}


```
2、OpenAi-ChatGpt集成

-- 第一步注册账号后获取API密钥
https://platform.openai.com/account/api-keys
![输入图片说明](https://foruda.gitee.com/images/1680076818553388299/755864ba_2334850.png "屏幕截图")
-- 第二步集成sdk

```
                 <dependency>
			<groupId>com.theokanning.openai-gpt3-java</groupId>
			<artifactId>client</artifactId>
			<version>0.9.0</version>
		</dependency>
```
代码片段

```
package com.gc.chatbot.service;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAiChatBiz {

    @Value("${open.ai.model}")
    private String openAiModel;
    @Autowired
    private OpenAiService openAiService;
    /**
     * 聊天
     * @param prompt
     * @return
     */
    public String chat(String prompt){
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(prompt)
                .model(openAiModel)
                .echo(true)
                .temperature(0.7)
                .topP(1d)
                .frequencyPenalty(0d)
                .presencePenalty(0d)
                .maxTokens(1000)
                .build();
        CompletionResult completionResult = openAiService.createCompletion(completionRequest);
        StringBuffer text=new StringBuffer();
        completionResult.getChoices().forEach(v->{
            text.append(v.getText()+"\n");
        });
        return text.toString();
    }
}
```


#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
