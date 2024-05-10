package com.example.telegrambot.config;

import com.example.telegrambot.service.BotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
@Component
@Slf4j
public class CreateBot {
    @Autowired
    BotService botService;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi botsApi=new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(botService);
        } catch (TelegramApiException e) {
            log.error("Error occured"+e.getMessage());
        }
    }
}
