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

    private final Map<Long, String> chatStates = new HashMap<>();
    private ArrayList<String> objects=new ArrayList<>();
    private final SMSandButtons settings=new SMSandButtons();
    private final Days days=new Days();

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
                AddObject(chatID, schedule, repo, text,1);
            }

            else if (chatStates.get(chatID).equals("TUESDAY")){
                AddObject(chatID, schedule, repo, text,2);
            }

            else if (chatStates.get(chatID).equals("WEDNESDAY")){
                AddObject(chatID, schedule, repo, text,3);
            }

            else if (chatStates.get(chatID).equals("THURSDAY")){
                AddObject(chatID, schedule, repo, text,4);
            }

            else if (chatStates.get(chatID).equals("FRIDAY")){
                AddObject(chatID, schedule, repo, text,5);
            }

            else if (chatStates.get(chatID).equals("SATURDAY")){
                AddObject(chatID, schedule, repo, text,6);
            }

            else
                settings.sendMessage(chatID, "Неправильный ввод!");

         }
        else if (update.hasCallbackQuery()){
            String callbackData=update.getCallbackQuery().getData();
            long chatID=update.getCallbackQuery().getMessage().getChatId();
            int messageID=update.getCallbackQuery().getMessage().getMessageId();

            if (callbackData.equals("START_BUTTON"))
                 days.Weekdays(chatID,checkDays(chatID));

            else if (callbackData.equals("MONDAY"))
                days.Monday(chatID,messageID);

            else if (callbackData.equals("TUESDAY"))
                days.Tuesday(chatID, messageID);

            else if (callbackData.equals("WEDNESDAY"))
                days.Wednesday(chatID, messageID);

            else if (callbackData.equals("THURSDAY"))
                days.Thursday(chatID, messageID);

            else if (callbackData.equals("FRIDAY"))
                days.Friday(chatID, messageID);

            else if (callbackData.equals("SATURDAY"))
                days.Saturday(chatID, messageID);

            else if (callbackData.equals("CHANGE"))
                changeExistData(chatID, messageID);

            else if (callbackData.equals("ADD")) {
                changeExistData(chatID, messageID);
                statusOfChange = "add";
            }
         }
    }

    private String statusOfChange="change";
    private void AddObject(long chatID, Schedule schedule, ScheduleRepo repo, String text,int day) {
        if (!text.equals("/stop")) {
            objects.add(text);
            settings.sendMessage(chatID,showObjects(objects) +"\nНапишите еще занятие или для остановки /stop:");
        }
        else {
            schedule.setObjects(objects);
            try {
                if (repo.userExist(chatID) && Objects.equals(statusOfChange, "change")) {
                    repo.updateQuery(chatID, schedule,day);
                    settings.sendMessage(chatID, showObjects(objects)+"\nВсе <b>успешно</b> обновлено !");
                    days.Weekdays(chatID,checkDays(chatID));
                }
                else if (repo.userExist(chatID) && Objects.equals(statusOfChange, "add")){
                    repo.addExistQuery(chatID,schedule,day);
                    settings.sendMessage(chatID, showObjects(objects)+"\nВсе <b>успешно</b> добавлено !");
                    days.Weekdays(chatID,checkDays(chatID));
                }
                else {
                    repo.addQuery(chatID, schedule,day);
                    settings.sendMessage(chatID, showObjects(objects)+"\nВсе <b>успешно</b> добавлено !");
                    days.Weekdays(chatID,checkDays(chatID));
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            finally {
                this.objects=new ArrayList<>();
                statusOfChange="change";
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
        settings.sendMessage(chatid,text);
    }


    private void changeExistData(long chatId,int messageId){
        String text="Теперь напиши <i>занятие</i> :";
        settings.sendEditMessage(chatId, messageId, text);
    }


    private void checkUser(long chatId,int messageId,int day) throws SQLException {
        Schedule schedule=new ScheduleRepo().findById(chatId,day);

        if (schedule != null){
            String text="У вас уже создано расписание на этот день\nХотите изменить или добавить?";
            InlineKeyboardMarkup reply = settings.ChangeAddButtons();
            settings.sendEditMessage(chatId, messageId,reply,text);
        }
        else {
            String text="Теперь напиши <i>занятие</i> :";
            settings.sendEditMessage( chatId, messageId,text);
        }
    }

    private int checkDays(long chatId) throws SQLException {
        List<List<String>> arrayObjects=new ScheduleRepo().showObjects(chatId);
        if (arrayObjects.isEmpty()) return 0;
        if (arrayObjects.size()==6) return 1;
        return 2;
    }

    private String showObjects(ArrayList<String> objects){
        StringBuilder sb=new StringBuilder();
        int order=1;
        for (String text:objects) {
            String modifiedText = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            sb.append("<b>").append(order).append(".</b>").append(modifiedText).append("\n");
            order++;
        }
        return sb.toString();
    }

    private final class Days {
        public void Weekdays(long chatID,int status){
            String text="";
            if (status==0) text="Давай начнем!\n\n<b>\uD83D\uDCCCСначала выбери день :</b>";
            else if (status==1){
                text="Поздравляю ты заполнил свое <b>расписание!</b>\uD83E\uDD29";
                settings.sendMessage(chatID, text);
                return;
            }
            else text="Добавь в другие дни!\n\n<b>\uD83D\uDCCCВыбери день :</b>";
            SendMessage message=new SendMessage(String.valueOf(chatID), text);

            InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows=new ArrayList<>();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            settings.SetMessageButtons(message, markup, rows, buttons);
        }

        public void Monday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "MONDAY");
            checkUser(chatId, messageId,1);
        }

        public void Tuesday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "TUESDAY");
            checkUser(chatId, messageId,2);
        }

        public void Wednesday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "WEDNESDAY");
            checkUser(chatId, messageId,3);
        }

        public void Thursday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "THURSDAY");
            checkUser(chatId, messageId,4);
        }

        public void Friday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "FRIDAY");
            checkUser(chatId, messageId,5);
        }

        public void Saturday(long chatId,int messageId) throws Exception {
            chatStates.put(chatId, "SATURDAY");
            checkUser(chatId, messageId,6);
        }
    }
    private final class SMSandButtons{
        public void sendMessage(long chatID, String send) {
            SendMessage botsend=new SendMessage(String.valueOf(chatID),send);
            botsend.setParseMode(ParseMode.HTML);
            try {
                execute(botsend);
            } catch (TelegramApiException e) {
                log.error("[Error occured]"+e.getMessage());
            }
        }

        public InlineKeyboardMarkup ChangeAddButtons() {
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
            return reply;
        }

        public void SetMessageButtons(SendMessage message, InlineKeyboardMarkup markup, List<List<InlineKeyboardButton>> rows, List<InlineKeyboardButton> buttons){
            InlineKeyboardButton button=new InlineKeyboardButton();

            button.setText("1.Понедельник");
            button.setCallbackData("MONDAY");

            buttons.add(button);

            button=new InlineKeyboardButton();
            button.setText("4.Четверг");
            button.setCallbackData("THURSDAY");

            buttons.add(button);
            rows.add(buttons);

            buttons=new ArrayList<>();
            button=new InlineKeyboardButton();
            button.setText("2.Вторник");
            button.setCallbackData("TUESDAY");

            buttons.add(button);

            button=new InlineKeyboardButton();
            button.setText("5.Пятница");
            button.setCallbackData("FRIDAY");

            buttons.add(button);
            rows.add(buttons);

            buttons=new ArrayList<>();
            button=new InlineKeyboardButton();
            button.setText("3.Среда");
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

        public void sendEditMessage(long chatId,int messageId,String text){
            EditMessageText message= EditMessageText.builder()
                    .messageId(messageId).chatId(chatId).text(text).build();

            message.setParseMode(ParseMode.HTML);
            try {
                execute(message);
            }catch (TelegramApiException t){
                log.error(t.getMessage());
            }
        }

        public void sendEditMessage(long chatId,int messageId,InlineKeyboardMarkup reply,String text){
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

    }

}

