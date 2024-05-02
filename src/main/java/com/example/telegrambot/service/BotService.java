package com.example.telegrambot.service;

import com.example.telegrambot.config.Bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class BotService extends TelegramLongPollingBot {
    final Bot bot;
    public BotService(Bot bot){
        this.bot=bot;
    }

    @Override
    public String getBotToken(){
        return bot.getToken();
    }


    @Override
    public String getBotUsername() {
        return bot.getBot_name();
    }

    public String getUsername(Update update){
        return update.getMessage().getFrom().getUserName();
    }

    @Override
    public void onUpdateReceived(Update update) {
         if (update.getMessage().hasText() && update.hasMessage()){
             String text=update.getMessage().getText();
             long chatID=update.getMessage().getChatId();

             switch (text){
                 case "/start":
                     String send="Hi,"+getUsername(update);
                     sendMessage(chatID,send);
                     break;
                 default:
                     sendMessage(chatID,"I don't know this command!");
             }
         }
    }

    private void sendMessage(long chatID, String send) {
        SendMessage botsend=new SendMessage(String.valueOf(chatID),send);
        try {
            execute(botsend);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
