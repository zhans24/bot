package com.example.telegrambot.service;

import com.example.telegrambot.config.Bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
@Component
@Slf4j
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
            log.error("Error occured"+e.getMessage());
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
        if (update.hasMessage() && update.getMessage().hasText() ){
            long chatID=update.getMessage().getChatId();

            String text=update.getMessage().getText();
             switch (text){
                 case "/start":
                    startCommand(update,chatID);
                     break;
                 case "/help":
                     helpCommand(chatID);
                     break;
                 default:
                     sendMessage(chatID,"I don't know this command!");
                     break;
             }
         }
         else if (update.hasCallbackQuery()){
             String callbackData=update.getCallbackQuery().getData();
             long chatID=update.getCallbackQuery().getMessage().getChatId();
            if (callbackData.equals("START_BUTTON")){
                 Weekdays(chatID);
             }
         }
    }

    private void startCommand(Update update,long chatID){
        String text="<b>Привет ,  "+update.getMessage().getFrom().getFirstName()+"! \uD83D\uDC4B</b>";
        SendMessage message=new SendMessage(String.valueOf(chatID), text);

        InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        List<InlineKeyboardButton> buttons=new ArrayList<>();

        InlineKeyboardButton button=new InlineKeyboardButton();

        button.setText("Создать расписание");
        button.setCallbackData("START_BUTTON");

        buttons.add(button);

        rows.add(buttons);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        message.setParseMode(ParseMode.HTML);

        try {
            execute(message);
        }catch(TelegramApiException e){
            log.error("[ERROR]"+e.getMessage());
        }
    }

    private void helpCommand(long chatid){
        String text= """
                Этот бот поможет вам составить и хранить расписание, которое будет повторяться каждую неделю📆

                Команды:
                👉/start - начать использование бота
                👉/help - показать это сообщение с описанием команд

                Насчёт вопросов, свяжитесь со мной:
                🌐 : @gazizhasik
                📞 : +7 (707) 200-50-24
                """;
        sendMessage(chatid,text);
    }

    private void sendMessage(long chatID, String send) {
        SendMessage botsend=new SendMessage(String.valueOf(chatID),send);
        try {
            execute(botsend);
        } catch (TelegramApiException e) {
            log.error("[Error occured]"+e.getMessage());
        }
    }

    private void Weekdays(long chatID){
        String text="Давай начнем!\n\n<b>\uD83D\uDCCCСначала выбери день :</b>";
        SendMessage message=new SendMessage(String.valueOf(chatID), text);

        InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        SetMessageButtons(message, markup, rows, buttons);
    }

    private void SetMessageButtons(SendMessage message,InlineKeyboardMarkup markup,List<List<InlineKeyboardButton>> rows,List<InlineKeyboardButton> buttons){
        InlineKeyboardButton button=new InlineKeyboardButton();

        button.setText("1.Понедельник");
        button.setCallbackData("MONDAY");

        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("4.Четверг");       // ЧЕТВЕРГ
        button.setCallbackData("TUESDAY");

        buttons.add(button);
        rows.add(buttons);

        buttons=new ArrayList<>();
        button=new InlineKeyboardButton();
        button.setText("2.Вторник");              // ВТОРНИК
        button.setCallbackData("WEDNESDAY");

        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("5.Пятница");            //ПЯТНИЦА
        button.setCallbackData("THURSDAY");

        buttons.add(button);
        rows.add(buttons);

        buttons=new ArrayList<>();
        button=new InlineKeyboardButton();
        button.setText("3.Среда");            //СРЕДА
        button.setCallbackData("WEDNESDAY");

        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("6.Суббота");
        button.setCallbackData("SATURDAY");

        buttons.add(button);
        rows.add(buttons);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        message.setParseMode(ParseMode.HTML);

        try {
            execute(message);
        }catch(TelegramApiException e){
            log.error("[ERROR]"+e.getMessage());
        }
    }




}
