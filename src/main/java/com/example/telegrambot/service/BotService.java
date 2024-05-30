package com.example.telegrambot.service;

import com.example.telegrambot.Repository.ScheduleRepo;
import com.example.telegrambot.config.Bot;

import com.example.telegrambot.models.Schedule;
import lombok.SneakyThrows;
import lombok.Synchronized;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.*;

@Component
@Slf4j
public class BotService extends TelegramLongPollingBot {
    private final Bot bot;
    @Deprecated
    public BotService(Bot bot){
        this.bot=bot;

        List<BotCommand> commands=new ArrayList<>();
        commands.add(new BotCommand("/start","Запустить бота"));
        commands.add(new BotCommand("/help","Вопросы"));
        commands.add(new BotCommand("/add","Добавить расписание"));
        commands.add(new BotCommand("/delete","Удалить из расписания"));
        commands.add(new BotCommand("/show","Посмотреть расписание"));
        commands.add(new BotCommand("/bye","Остановка бота"));
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

    private final Map<Long, String> dayStates = new HashMap<>();
    private final Map<Long, String> userStates = new HashMap<>();
    private List<String> objects= Collections.synchronizedList(new ArrayList<>());
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
            switch (text) {
                case "/start" -> startCommand(update, chatID);
                case "/help" -> helpCommand(chatID);
                case "/add" -> addCommand(chatID);
                case "/delete" -> deleteCommand(chatID);
                case "/show" -> showCommand(chatID);
                case "/bye" -> {
                    settings.sendMessage(chatID, "<b>Пока!</b>\nБот остановлен для запуска /start");
                    return;
                }
                default -> {
                    if (userStates.get(chatID)==null || !userStates.get(chatID).equals("/del") && !userStates.get(chatID).equals("/add"))
                        settings.sendMessage(chatID, "Выберите команду!");
                }

            }
            if (userStates.get(chatID)!=null) {
                if (userStates.get(chatID).equals("/add")) {
                    switch (dayStates.get(chatID)) {
                        case "MONDAY" -> AddObject(chatID, schedule, repo, text, 1);
                        case "TUESDAY" -> AddObject(chatID, schedule, repo, text, 2);
                        case "WEDNESDAY" -> AddObject(chatID, schedule, repo, text, 3);
                        case "THURSDAY" -> AddObject(chatID, schedule, repo, text, 4);
                        case "FRIDAY" -> AddObject(chatID, schedule, repo, text, 5);
                        case "SATURDAY" -> AddObject(chatID, schedule, repo, text, 6);
                        default -> {
                            if (!containsCommand(text))
                                settings.sendMessage(chatID, "Выбери кнопку на сообщение!");
                        }
                    }
                } else if (userStates.get(chatID).equals("/del")) {
                    switch (text) {
                        case "1.Пн" -> DeleteObject(chatID, 1);
                        case "2.Вт" -> DeleteObject(chatID, 2);
                        case "3.Ср" -> DeleteObject(chatID, 3);
                        case "4.Чт" -> DeleteObject(chatID, 4);
                        case "5.Пт" -> DeleteObject(chatID, 5);
                        case "6.Сб" -> DeleteObject(chatID, 6);
                        default -> {
                            if (!containsCommand(text))
                                settings.sendMessage(chatID,"Выбери из кнопок!");
                        }
                    }
                }
            }

        }
        else if (update.hasCallbackQuery()){
            String callbackData=update.getCallbackQuery().getData();
            long chatID=update.getCallbackQuery().getMessage().getChatId();
            int messageID=update.getCallbackQuery().getMessage().getMessageId();

            switch (callbackData) {
                case "START_BUTTON" -> days.Weekdays(chatID, 0);
                case "MONDAY" -> days.Monday(chatID, messageID);
                case "TUESDAY" -> days.Tuesday(chatID, messageID);
                case "WEDNESDAY" -> days.Wednesday(chatID, messageID);
                case "THURSDAY" -> days.Thursday(chatID, messageID);
                case "FRIDAY" -> days.Friday(chatID, messageID);
                case "SATURDAY" -> days.Saturday(chatID, messageID);
                case "CHANGE" -> changeExistData(chatID, messageID);
                case "ADD" -> {
                    changeExistData(chatID, messageID);
                    statusOfChange = "add";
                }
            }

        }

    }
    private boolean containsCommand(String text){
        String[] commands={"/start","/show","/delete","/add","/help"};
        for (String s:commands) {
            if (text.equals(s))
                return true;
        }
        return false;
    }

    private void showCommand(long chatId) throws SQLException {
        userStates.put(chatId, "/show");
        settings.sendMessage(chatId, showObjects(chatId));
    }

    private String showObjects(long chatID) throws SQLException {
        List<List<String>> array=new ScheduleRepo().showObjects(chatID);
        StringBuilder sb=new StringBuilder();
        String[] days={"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

        for (int i=0;i<array.size();i++) {
            List<String> objects=array.get(i);
            sb.append("<b><i>").append(days[i]).append(":</i></b>\n");
            if (!objects.isEmpty()) {
                int count=1;
                for (String o : objects) {
                    sb.append("    ").append("<b>").append(count).append(".</b>").append(o).append("\n");
                    count++;
                }
            }else {
                sb.append("    ").append("\uD83D\uDDD1️\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void deleteCommand(long chatID) throws SQLException {
        userStates.put(chatID, "/del");
        String text=showObjects(chatID)+"\nДля <b>удаления</b> выберите день:";
        SendMessage send=new SendMessage(String.valueOf(chatID), text);

        ReplyKeyboardMarkup markup=new ReplyKeyboardMarkup();
        List<KeyboardRow> buttons = new ArrayList<>();
        KeyboardRow button1=new KeyboardRow();
        KeyboardRow button2=new KeyboardRow();

        button1.add("1.Пн");
        button1.add("2.Вт");
        button1.add("3.Ср");


        buttons.add(button1);

        button2.add("4.Чт");
        button2.add("5.Пт");
        button2.add("6.Сб");

        buttons.add(button2);

        markup.setKeyboard(buttons);
        markup.setOneTimeKeyboard(true);
        send.setReplyMarkup(markup);
        send.setParseMode(ParseMode.HTML);
        try {
            execute(send);
        }catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

    }

    private void addCommand(long chatID) {
        userStates.put(chatID, "/add");
        dayStates.put(chatID, "");
        days.Weekdays(chatID, 0);
    }
    private String statusOfChange="change";
    synchronized private void AddObject(long chatID, Schedule schedule, ScheduleRepo repo, String text,int day) {
        if (!text.equals("/stop")) {
            if (text.matches("\\d+\\s*")) {
                if ((Integer.parseInt(text) - 1) < objects.size()) {
                    objects.remove(Integer.parseInt(text) - 1);
                    settings.sendMessage(chatID, showObjects(objects) + "\n<i>Для удаление введите число занятия для остановки</i> /stop\n<b>Напишите еще занятия:</b>");
                } else
                    settings.sendMessage(chatID, "Число не соответствует количеству занятий!\nДля остановки /stop");
            }
            else {
                objects.add(text);
                settings.sendMessage(chatID, showObjects(objects) + "\nДля удаление введите число занятия для остановки /stop\n<b>Напишите еще занятия:</b>");
            }
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
            this.objects=new ArrayList<>();
            statusOfChange="change";
            dayStates.put(chatID, "");
            userStates.put(chatID, "/add");
        }
    }
    private void DeleteObject(long chatId,int day) throws SQLException {
        ScheduleRepo repo=new ScheduleRepo();
        if (repo.removeQuery(chatId, day)) {
            settings.sendMessage(chatId, "<b>Успешно удалено!</b>");
        }
        else settings.sendMessage(chatId, "Этот день <b>пустой!</b>");
    }

    private void startCommand(Update update,long chatID){
        userStates.put(chatID, "/start");
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
        userStates.put(chatid, "/help");
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
        if (arrayObjects.isEmpty())
            return 0;

        int count=0;
        for (List<String> o:arrayObjects) {
            if (!o.isEmpty()) count++;
        }
        return (count==6) ? 1 : 2;
    }

    private String showObjects(List<String> objects){
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
            userStates.put(chatID, "/add");
            String text;
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
            dayStates.put(chatId, "MONDAY");
            checkUser(chatId, messageId,1);
        }

        public void Tuesday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "TUESDAY");
            checkUser(chatId, messageId,2);
        }

        public void Wednesday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "WEDNESDAY");

            checkUser(chatId, messageId,3);
        }

        public void Thursday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "THURSDAY");

            checkUser(chatId, messageId,4);
        }

        public void Friday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "FRIDAY");

            checkUser(chatId, messageId,5);
        }

        public void Saturday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "SATURDAY");

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