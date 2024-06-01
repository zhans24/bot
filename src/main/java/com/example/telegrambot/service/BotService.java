package com.example.telegrambot.service;

import com.example.telegrambot.Repository.ScheduleRepo;
import com.example.telegrambot.config.Bot;

import com.example.telegrambot.models.Schedule;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Component
@Slf4j
@EnableScheduling
public class BotService extends TelegramLongPollingBot {
    private final Bot bot;
    @Deprecated
    public BotService(Bot bot){
        this.bot=bot;

        List<BotCommand> commands=new ArrayList<>();
        commands.add(new BotCommand("/start","Запустить бота"));
        commands.add(new BotCommand("/help","Вопросы"));
        commands.add(new BotCommand("/tomorrow","Занятия на завтра"));
        commands.add(new BotCommand("/today","Занятия сегодня"));
        commands.add(new BotCommand("/show","Посмотреть расписание"));
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
    private final Map<Long,ArrayList<String>> objects= new HashMap<>();
    private final SMSandButtons settings=new SMSandButtons();
    private final Days days=new Days();

    @SneakyThrows
    @Override //Обновления от юзера (смс,кнопки)
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() ){
            long chatID=update.getMessage().getChatId();
            Schedule schedule=new Schedule();

            String text=update.getMessage().getText();

            objects.computeIfAbsent(chatID, value -> new ArrayList<>());

            switch (text) {
                case "/start" -> startCommand(update, chatID);
                case "/help" -> helpCommand(chatID);
                case "/add" -> addCommand(chatID);
                case "/delete" -> deleteCommand(chatID);
                case "/show" -> showCommand(chatID);
                case "/tomorrow" -> tomorrow();
                case "/today" -> today();
                default -> {
                    if (userStates.get(chatID)==null || !userStates.get(chatID).equals("/del") && !userStates.get(chatID).equals("/add")){
                        SendMessage message=new SendMessage(String.valueOf(chatID),"Выберите команду!");
                        try {
                            int messageId=execute(message).getMessageId();
                            Thread.sleep(1000);

                            execute(new DeleteMessage(String.valueOf(chatID),messageId));

                        }catch (TelegramApiException t){
                            log.error(t.getMessage());
                        }
                    }
                }

            }
            if (userStates.get(chatID)!=null) {
                if (userStates.get(chatID).equals("/add")) {
                    switch (dayStates.get(chatID)) {
                        case "MONDAY" -> AddObject(chatID, schedule, text, 1);
                        case "TUESDAY" -> AddObject(chatID, schedule, text, 2);
                        case "WEDNESDAY" -> AddObject(chatID, schedule, text, 3);
                        case "THURSDAY" -> AddObject(chatID, schedule, text, 4);
                        case "FRIDAY" -> AddObject(chatID, schedule, text, 5);
                        case "SATURDAY" -> AddObject(chatID, schedule, text, 6);
                        default -> {
                            if (containsCommand(text)){
                                SendMessage message=new SendMessage(String.valueOf(chatID),"Выбери кнопку на сообщение!");
                                try {
                                    int messageId=execute(message).getMessageId();
                                    Thread.sleep(1000);

                                    execute(new DeleteMessage(String.valueOf(chatID),messageId));

                                }catch (TelegramApiException t){
                                    log.error(t.getMessage());
                                }
                            }

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
                            if (containsCommand(text)){
                                SendMessage message=new SendMessage(String.valueOf(chatID),"Выбери из кнопок!");
                                try {
                                    int messageId=execute(message).getMessageId();
                                    Thread.sleep(1000);

                                    execute(new DeleteMessage(String.valueOf(chatID),messageId));

                                }catch (TelegramApiException t){
                                    log.error(t.getMessage());
                                }
                            }
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
                case "START_BUTTON", "BACK" -> {
                    userStates.put(chatID, "/start");
                    menuCommand(chatID,messageID);
                }
                case "MONDAY" -> days.Monday(chatID, messageID);
                case "TUESDAY" -> days.Tuesday(chatID, messageID);
                case "WEDNESDAY" -> days.Wednesday(chatID, messageID);
                case "THURSDAY" -> days.Thursday(chatID, messageID);
                case "FRIDAY" -> days.Friday(chatID, messageID);
                case "SATURDAY" -> days.Saturday(chatID, messageID);
                case "CHANGE" -> {
                    changeExistData(chatID, messageID);
                    userStates.put(chatID, "/add");
                    objects.put(chatID, new ArrayList<>());
                }
                case "ADD" -> {
                    userStates.put(chatID, "/add");
                    changeExistData(chatID, messageID);
                }

                case "SHOW" -> {
                    InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows=new ArrayList<>();
                    List<InlineKeyboardButton> buttons=new ArrayList<>();
                    InlineKeyboardButton button=new InlineKeyboardButton();
                    button.setText("⬅Назад");
                    button.setCallbackData("BACK");
                    buttons.add(button);
                    rows.add(buttons);
                    markup.setKeyboard(rows);
                    settings.sendEditMessage(chatID, messageID, markup,showAddedObjects(chatID));
                }

                case "ADDfromMenu" -> {
                    days.Weekdays(messageID, chatID);
                }

                case "DELETE" -> deleteCommand(chatID);

            }

        }
    }


    // запуск программы
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

    private void menuCommand(long chatId,int messageId) throws TelegramApiException {
        EditMessageText message=new EditMessageText();

        InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows=new ArrayList<>();
        List<InlineKeyboardButton> buttons=new ArrayList<>();

        InlineKeyboardButton button=new InlineKeyboardButton();

        button.setText("Показать");
        button.setCallbackData("SHOW");
        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("Добавить");
        button.setCallbackData("ADDfromMenu");
        buttons.add(button);

        button=new InlineKeyboardButton();
        button.setText("Удалить");
        button.setCallbackData("DELETE");
        buttons.add(button);

        rows.add(buttons);

        markup.setKeyboard(rows);

        message.setChatId(chatId);
        message.setText("Что вы хотите сделать с расписанием?");
        message.setReplyMarkup(markup);
        message.setMessageId(messageId);

        execute(message);
    }


    //показывает текущие все расписания который добавил юзер
    private void showCommand(long chatId) throws SQLException {
        userStates.put(chatId, "/show");
        settings.sendMessage(chatId, showAddedObjects(chatId));
    }



    //удаляет обьекты одного из дней
    private void deleteCommand(long chatID) throws SQLException {
        userStates.put(chatID, "/del");
        String text=showAddedObjects(chatID)+"\nДля <b>удаления</b> выберите день:";
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


    // добавляет обьекты в один из дней
    private void addCommand(long chatID) {
        userStates.put(chatID, "/add");
        dayStates.put(chatID, "");
        days.Weekdays(chatID, 0);
    }


    // инструкция для использования или помощь
    private void helpCommand(long chatid){
        userStates.put(chatid, "/help");
        String text= """
            Этот бот поможет вам составить и хранить расписание, которое будет повторяться каждую неделю📆

            Команды:
            👉/start - начать использование бота
            👉/show - показать добавленные объекты расписания
            👉/delete - удалить объекты из определенного дня
            👉/add - добавить объекты в выбранный день
            👉/help - показать это сообщение с описанием команд

            Насчёт вопросов, свяжитесь со мной:
            🌐 : @gazizhasik
            📞 : +7 (707) 200-50-24
            """;
        settings.sendMessage(chatid,text);
    }




    /**
     * Отвечает за добавление,обновление информации о занятиях в расписании в базе данных
     * @param chatID - Id юзера который работает с ботом
     * @param text - текст который написал юзер
     * @param day - для определения с каким днем работает юзер
     */
    private void AddObject(long chatID, Schedule schedule, String text,int day) {
        if (!text.equals("/stop")) {
            if (text.matches("\\d+\\s*")) {
                if ((Integer.parseInt(text) - 1) < objects.get(chatID).size()) {
                    ArrayList<String> objectsList = objects.get(chatID);

                    if (objectsList != null && Integer.parseInt(text) - 1 < objectsList.size()) {
                        objectsList.remove(Integer.parseInt(text) - 1);
                        objects.put(chatID, objectsList);
                    }
                    settings.sendDisappearingMessages(chatID, showActiveObjects(objects.get(chatID),"add") + "\n<i>Для удаление введите число занятия для остановки</i> /stop\n<b>Напишите еще занятия:</b>");
                } else
                    settings.sendDisappearingMessages(chatID, "Число не соответствует количеству занятий!\nДля остановки /stop");
            }
            else {
                ArrayList<String> objectsList = objects.get(chatID);
                if (objectsList != null) {
                    objectsList.add(text);
                    objects.put(chatID, objectsList);
                }
                settings.sendDisappearingMessages(chatID, showActiveObjects(objects.get(chatID),"add") + "\nДля удаление введите число занятия для остановки /stop\n<b>Напишите еще занятия:</b>");
            }
        }
        else {
            schedule.setObjects(objects.get(chatID));
            try {
                if (new ScheduleRepo().userExist(chatID)) {
                    new ScheduleRepo().updateQuery(chatID, schedule,day);

                    SendMessage message=new SendMessage(String.valueOf(chatID),showActiveObjects(objects.get(chatID),"add")+"\nВсе <b>успешно</b> обновлено !");

                    List<List<InlineKeyboardButton>> rows=new ArrayList<>();
                    List<InlineKeyboardButton> buttons=new ArrayList<>();
                    InlineKeyboardButton button=new InlineKeyboardButton();

                    button.setText("Показать");
                    button.setCallbackData("SHOW");

                    rows.add(new ArrayList<>(List.of(button)));

                    button=new InlineKeyboardButton();
                    button.setText("⬅Назад");
                    button.setCallbackData("BACK");
                    buttons.add(button);

                    button=new InlineKeyboardButton();
                    button.setText("Добавлять дальше➡");
                    button.setCallbackData("NEXT");
                    buttons.add(button);

                    rows.add(buttons);

                    InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
                    markup.setKeyboard(rows);

                    message.setReplyMarkup(markup);
                    message.setParseMode(ParseMode.HTML);

                    execute(message);
                }
                else {
                    new ScheduleRepo().addQuery(chatID, schedule,day);
                    settings.sendMessage(chatID, showActiveObjects(objects.get(chatID),"add")+"\nВсе <b>успешно</b> добавлено !");
                    days.Weekdays(chatID,checkDays(chatID));
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            }
            this.objects.put(chatID, new ArrayList<>());
            dayStates.put(chatID, "");
            userStates.put(chatID, "/add");
        }
    }

    /**
     * Отвечает за удаление информации о занятиях для определенного дня из базы данных.
     * @param chatId - ID пользователя, работающего с ботом.
     * @param day - день, для которого осуществляется удаление данных.
    */
    private void DeleteObject(long chatId,int day) throws SQLException {
        ScheduleRepo repo=new ScheduleRepo();
        if (repo.removeQuery(chatId, day)) {
            settings.sendMessage(chatId, "<b>Успешно удалено!</b>");
        }
        else settings.sendMessage(chatId, "Этот день <b>пустой!</b>");
    }




    // Для изменения расписания который уже создан
    private void changeExistData(long chatId,int messageId){
        String text="Теперь напиши <i>занятие</i> :";
        settings.sendEditMessage(chatId, messageId, text);
    }




    /**
     * Проверяет информацию о расписании пользователя для определенного дня.
     * @param chatId - ID пользователя, для которого осуществляется проверка
     * @param messageId - ID сообщения, чтобы изменить это же сообщения в другое сообщение
     * @param day - день, для которого проверяется информация о расписании.
    */
    private void checkUserData(long chatId,int messageId,int day) throws SQLException {
        Schedule schedule=new ScheduleRepo().findById(chatId,day);

        if (schedule != null && !schedule.getObjects().isEmpty()){
            userStates.put(chatId, "/start");
            String text="У вас уже создано расписание на этот день\nХотите изменить или добавить?";
            InlineKeyboardMarkup reply = settings.ChangeAddButtons();
            objects.put(chatId, schedule.getObjects());
            settings.sendEditMessage(chatId, messageId,reply,text);
        }
        else {
            userStates.put(chatId, "/add");
            String text="Теперь напиши <i>занятие</i> :";
            settings.sendEditMessage( chatId, messageId,text);
        }
    }



    /**
     * Проверяет, добавлены ли все дни расписания для определенного пользователя или есть пустые дни
     * @param chatId - ID пользователя, для которого осуществляется проверка
     * @return 0 - если нет добавленных дней, 1 - если все дни заполнены, 2 - если есть пустые дни
     */
    @NotNull
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

    // Показывает занятия которые добавятся в таблицу в методе AddObject
    @NotNull
    private String showActiveObjects(ArrayList<String> objects,String status){
        StringBuilder sb=new StringBuilder();
        int order=1;
        for (String text:objects) {
            String modifiedText = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            if (status.equals("add")) sb.append("<b>").append(order).append(".</b>").append(modifiedText).append("\n");
            else sb.append("<b>        ").append(order).append(".</b>").append(modifiedText).append("\n");
            order++;
        }
        return sb.toString();
    }


    // Покажет итоговый добавленное расписание в таблицу
    @NotNull
    private String showAddedObjects(long chatID) throws SQLException {
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


    // Для проверки сообщения юзера ввел ли он команду
    @NotNull
    private boolean containsCommand(String text){
        String[] commands={"/start","/show","/delete","/add","/help"};
        for (String s:commands) {
            if (text.equals(s))
                return false;
        }
        return true;
    }



    // Отправка сообщений для всех пользователей в определенное время
    @Scheduled(cron = "0 0 21 * * *")
    private void tomorrow() throws SQLException, TelegramApiException, InterruptedException {
        LocalDate tomorrow=LocalDate.now().plusDays(1);

        ArrayList<Schedule> schedules;

        String[] days={"В <u><b>понедельник</b></u>","Во <u><b>вторник</b></u>","В <u><b>среду</b></u>","В <u><b>четверг</b></u>","В <u><b>пятницу</b></u>","В <u><b>субботу</b></u>"};

        int dayOfWeekNumber = tomorrow.getDayOfWeek().getValue();

        schedules = switch (dayOfWeekNumber) {
            case 1 -> new ScheduleRepo().findDayObjects(1);
            case 2 -> new ScheduleRepo().findDayObjects(2);
            case 3 -> new ScheduleRepo().findDayObjects(3);
            case 4 -> new ScheduleRepo().findDayObjects(4);
            case 5 -> new ScheduleRepo().findDayObjects(5);
            default -> new ScheduleRepo().findDayObjects(6);
        };
        for (Schedule user:schedules) {
            if (!user.getObjects().isEmpty()) {
                String sb = "<b>\uD83D\uDCCCЗавтрашние занятия:</b>\n" +
                        showActiveObjects(user.getObjects(),"show") +
                        "\n\n<b>Не забудь!</b>\uD83D\uDE09";

                settings.sendMessage(user.getChatID(), sb);
            }
            else
                if (dayOfWeekNumber<6)
                    settings.sendMessage(user.getChatID(), days[dayOfWeekNumber-1] +" у вас нет расписаний!\nЗаполни, чтобы получать уведомления каждый день\uD83D\uDE0A");
                else {
                    SendMessage send=new SendMessage(String.valueOf(user.getChatID()),"<b>Отдыхай завтра воскресенье\uD83D\uDE09</b>");
                    send.setParseMode(ParseMode.HTML);
                    int messageId=execute(send).getMessageId();
                    Thread.sleep(1500);

                    menuCommand(user.getChatID(), messageId);
                }


        }

    }

    private void today() throws SQLException {
        LocalDate tomorrow=LocalDate.now();

        ArrayList<Schedule> schedules;

        int dayOfWeekNumber = tomorrow.getDayOfWeek().getValue();

        schedules = switch (dayOfWeekNumber) {
            case 1 -> new ScheduleRepo().findDayObjects(1);
            case 2 -> new ScheduleRepo().findDayObjects(2);
            case 3 -> new ScheduleRepo().findDayObjects(3);
            case 4 -> new ScheduleRepo().findDayObjects(4);
            case 5 -> new ScheduleRepo().findDayObjects(5);
            default -> new ScheduleRepo().findDayObjects(6);
        };
        for (Schedule user:schedules) {
            if (!user.getObjects().isEmpty()) {
                String sb = "<b>\uD83D\uDCCCСегодня у вас:</b>\n" +
                        showActiveObjects(user.getObjects(),"show") +
                        "\n<b>Не забудь!</b>\uD83D\uDE09";

                settings.sendMessage(user.getChatID(), sb);
            }
            else
                settings.sendMessage(user.getChatID(), "На <b>сегодня</b> у вас нет расписаний!\nЗаполни, чтобы получать уведомления каждый день\uD83D\uDE0A");
        }

    }


    private final class Days {
        public void Weekdays(long chatID,int status){
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

        public void Weekdays(int messageId,long chatID){
            String text="Добавь в другие дни!\n\n<b>\uD83D\uDCCCВыбери день :</b>";
            EditMessageText message=new EditMessageText();
            message.setText(text);
            message.setChatId(chatID);
            message.setMessageId(messageId);

            InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows=new ArrayList<>();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            settings.SetMessageButtons(message, markup, rows, buttons);
        }

        public void Monday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "MONDAY");
            userStates.put(chatId, "/add");
            checkUserData(chatId, messageId,1);
        }

        public void Tuesday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "TUESDAY");
            userStates.put(chatId, "/add");

            checkUserData(chatId, messageId,2);
        }

        public void Wednesday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "WEDNESDAY");
            userStates.put(chatId, "/add");

            checkUserData(chatId, messageId,3);
        }

        public void Thursday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "THURSDAY");
            userStates.put(chatId, "/add");

            checkUserData(chatId, messageId,4);
        }

        public void Friday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "FRIDAY");
            userStates.put(chatId, "/add");

            checkUserData(chatId, messageId,5);
        }

        public void Saturday(long chatId,int messageId) throws Exception {
            dayStates.put(chatId, "SATURDAY");
            userStates.put(chatId, "/add");

            checkUserData(chatId, messageId,6);
        }
    }
    private final class SMSandButtons{
        public void sendDisappearingMessages(long chatId,String send){
            SendMessage botsend=new SendMessage(String.valueOf(chatId),send);
            botsend.setParseMode(ParseMode.HTML);

            try {
                int messageid=execute(botsend).getMessageId();
                Thread.sleep(2000);
                execute(new DeleteMessage(String.valueOf(chatId),messageid));
            } catch (Exception e) {
                log.error("[Error occured]"+e.getMessage());
            }
        }

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

            button=new InlineKeyboardButton();
            button.setText("⬅Назад");
            button.setCallbackData("BACK");

            rows.add(row);
            rows.add(new ArrayList<>(List.of(button)));

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

        public void SetMessageButtons(EditMessageText message, InlineKeyboardMarkup markup, List<List<InlineKeyboardButton>> rows, List<InlineKeyboardButton> buttons){
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