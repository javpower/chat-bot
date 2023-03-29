package com.gc.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping
public class IndexHandler {

    @GetMapping("/index")
    public String index(Model model){
        return "index";
    }

    @GetMapping("/edge")
    public String edge(Model model){
        return "edge";
    }
}

