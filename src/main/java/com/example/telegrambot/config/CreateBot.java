/**
 * Этот код на Java представляет собой конфигурационный класс Spring,
 * который инициализирует и регистрирует Telegram бота при запуске приложения.
 */


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
    @Autowired // осуществляется внедрение зависимости бина BotService в класс CreateBot
    BotService botService;

    @EventListener({ContextRefreshedEvent.class}) // init будет вызван при событии ContextRefreshedEvent
    public void init() throws TelegramApiException {
        // Создаем экземпляр TelegramBotsApi для регистрации бота
        TelegramBotsApi botsApi=new TelegramBotsApi(DefaultBotSession.class);

        try {
            // Регистрируем бота , чтобы подключться к Telegram и принимать обновления
            botsApi.registerBot(botService);
        } catch (TelegramApiException e) {
            log.error("Error occured"+e.getMessage());
        }
    }
}
