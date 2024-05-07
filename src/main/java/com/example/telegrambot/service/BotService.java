package com.example.telegrambot.service;

import com.example.telegrambot.config.Bot;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
@Component
public class BotService extends TelegramLongPollingBot {
    private final Bot bot;


    public BotService(Bot bot){
        this.bot=bot;

        List<BotCommand> commands=new ArrayList<>();
        commands.add(new BotCommand("/start","Launch bot"));
        commands.add(new BotCommand("/help","Questions about bot"));
        try {
            this.execute(new SetMyCommands(commands,new BotCommandScopeDefault(),null));
        }catch (TelegramApiException e){
        }


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
                 case "/help":
                     helpCommand(update,chatID);
                     break;
                 default:
                     sendMessage(chatID,"I don't know this command!");
                     break;
             }


         }
    }

    private void helpCommand(Update update,long chatid){
        String text="Write about problems to @"+getUsername(update);
        sendMessage(chatid,text);
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
