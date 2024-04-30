package com.example.telegrambot.config;

import com.example.telegrambot.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class CreateBot {
    @Autowired
    BotService botService;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi botsApi=new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(botService);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
