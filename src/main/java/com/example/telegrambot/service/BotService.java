package com.example.telegrambot.service;

import com.example.telegrambot.Repository.ScheduleRepo;
import com.example.telegrambot.config.Bot;

import com.example.telegrambot.models.Schedule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.*;

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

    private Map<Long, String> chatStates = new HashMap<>();
    private ArrayList<ArrayList<String>> objects=new ArrayList<>();
    private ArrayList<Integer> days = new ArrayList<>(List.of(0, 0, 0, 0, 0, 0));
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() ){
            long chatID=update.getMessage().getChatId();
            Schedule schedule=new Schedule();
            ScheduleRepo repo=new ScheduleRepo();

            String text=update.getMessage().getText();

            if (text.equals("/start"))
                startCommand(update, chatID);

            else if (text.equals("/help"))
                helpCommand(chatID);

            else if (chatStates.get(chatID).equals("MONDAY")){
                days.set(0, 1);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,1);
            }

            else if (chatStates.get(chatID).equals("TUESDAY")){
                days.set(1, 2);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,2);
            }

            else if (chatStates.get(chatID).equals("WEDNESDAY")){
                days.set(2, 3);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,3);
            }

            else if (chatStates.get(chatID).equals("THURSDAY")){
                days.set(3,4);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,4);
            }

            else if (chatStates.get(chatID).equals("FRIDAY")){
                days.set(4,5);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,5);
            }

            else if (chatStates.get(chatID).equals("SATURDAY")){
                days.set(5,6);
                schedule.setDays(days);

                AddObject(chatID, schedule, repo, text,6);
            }

            else
                sendMessage(chatID, "Неправильный ввод!");

         }
        else if (update.hasCallbackQuery()){
            String callbackData=update.getCallbackQuery().getData();
            long chatID=update.getCallbackQuery().getMessage().getChatId();
            int messageID=update.getCallbackQuery().getMessage().getMessageId();

            if (callbackData.equals("START_BUTTON"))
                 Weekdays(chatID);

            else if (callbackData.equals("MONDAY"))
                Monday(chatID,messageID);

            else if (callbackData.equals("TUESDAY"))
                Tuesday(chatID, messageID);

            else if (callbackData.equals("WEDNESDAY"))
                Wednesday(chatID, messageID);

            else if (callbackData.equals("THURSDAY"))
                Thursday(chatID, messageID);

            else if (callbackData.equals("FRIDAY"))
                Friday(chatID, messageID);

            else if (callbackData.equals("SATURDAY"))
                Saturday(chatID, messageID);

            else if (callbackData.equals("CHANGE"))
                changeData(chatID, messageID);
         }
    }

    private void AddObject(long chatID, Schedule schedule, ScheduleRepo repo, String text,int day) {
        ArrayList<String> object=new ArrayList<>();
        if (!text.equals("/stop")) {
            object.add(text);
            sendMessage(chatID, "Напишите еще занятие или для остановки /stop:");
        }
        else {
            try {
                if (repo.findById(chatID,day) != null ) {
                    objects.set(day-1,object );
                    schedule.setObjects(objects);
                    repo.updateQuery(chatID, schedule);
                    sendMessage(chatID, "Все <b>успешно</b> обновлено !");
                }
                else {
                    objects.add(object);
                    schedule.setObjects(objects);
                    repo.addQuery(chatID, schedule);
                    sendMessage(chatID, "Все <b>успешно</b> добавлено !");
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            finally {
                this.objects=new ArrayList<>();
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
        botsend.setParseMode(ParseMode.HTML);
        try {
            execute(botsend);
        } catch (TelegramApiException e) {
            log.error("[Error occured]"+e.getMessage());
        }
    }

    private void sendMessage(long chatID, String send,ReplyKeyboardMarkup reply) {
        SendMessage botsend=new SendMessage(String.valueOf(chatID),send);
        botsend.setParseMode(ParseMode.HTML);
        botsend.setReplyMarkup(reply);

        try {
            execute(botsend);
        } catch (TelegramApiException e) {
            log.error("[Error occured]"+e.getMessage());
        }
    }

    private void sendEditMessage(long chatId,int messageId,String text){
        EditMessageText message= EditMessageText.builder()
                .messageId(messageId).chatId(chatId).text(text).build();

        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
        }catch (TelegramApiException t){
            log.error(t.getMessage());
        }
    }

    private void sendEditMessage(long chatId,int messageId,InlineKeyboardMarkup reply,String text){
        EditMessageText message= EditMessageText.builder()
                .messageId(messageId).chatId(chatId).text(text).build();
        message.setReplyMarkup(reply);

        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
        }catch (TelegramApiException t){
            log.error(t.getMessage());
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
        button.setCallbackData("THURSDAY");

        buttons.add(button);
        rows.add(buttons);

        buttons=new ArrayList<>();
        button=new InlineKeyboardButton();
        button.setText("2.Вторник");              // ВТОРНИК
        button.setCallbackData("TUESDAY");

        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("5.Пятница");            //ПЯТНИЦА
        button.setCallbackData("FRIDAY");

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





    private void changeData(long chatId,int messageId){
        String text="Теперь напиши <i>занятие</i> :";
        sendEditMessage(chatId, messageId, text);
    }

    private void checkUser(long chatId,int messageId,int day) throws SQLException {
        ScheduleRepo repo=new ScheduleRepo();
        Schedule schedule=repo.findById(chatId,day);

        if (schedule != null){
            String text="У вас уже создано расписание на этот день\nХотите изменить или добавить?";

            InlineKeyboardMarkup reply=new InlineKeyboardMarkup();
            List<InlineKeyboardButton> row=new ArrayList<>();
            List<List<InlineKeyboardButton>> rows=new ArrayList<>();
            InlineKeyboardButton button=new InlineKeyboardButton();

            button.setText("1.Изменить");
            button.setCallbackData("CHANGE");

            row.add(button);

            button=new InlineKeyboardButton();
            button.setText("2.Добавить");
            button.setCallbackData("ADD");

            row.add(button);

            rows.add(row);

            reply.setKeyboard(rows);

            sendEditMessage(chatId, messageId,reply,text);
        }
        else {
            String text="Теперь напиши <i>занятие</i> :";

            sendEditMessage( chatId, messageId,text);
        }
    }

    private void Monday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "MONDAY");
        checkUser(chatId, messageId,1);
    }

    private void Tuesday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "TUESDAY");
        checkUser(chatId, messageId,2);
    }

    private void Wednesday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "WEDNESDAY");
        checkUser(chatId, messageId,3);
    }

    private void Thursday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "THURSDAY");
        checkUser(chatId, messageId,4);
    }

    private void Friday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "FRIDAY");
        checkUser(chatId, messageId,5);
    }

    private void Saturday(long chatId,int messageId) throws Exception {
        chatStates.put(chatId, "SATURDAY");
        checkUser(chatId, messageId,6);
    }

}
