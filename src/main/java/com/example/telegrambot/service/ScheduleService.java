package com.example.telegrambot.service;

import com.example.telegrambot.Repository.ScheduleRepo;
import com.example.telegrambot.models.Schedule;
import org.apache.logging.log4j.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class ScheduleService {
    private final ScheduleRepo repo;
    public ScheduleService(ScheduleRepo repo){
        this.repo=repo;
    }

    public void add(Schedule schedule){
        repo.save(schedule);
    }

    public Schedule findById(long chatID){
        return repo.findById(chatID).orElse(null);
    }

    public void delete(long chatID){
        repo.deleteById(chatID);
    }



}
