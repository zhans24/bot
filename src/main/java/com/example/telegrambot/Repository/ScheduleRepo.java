package com.example.telegrambot.Repository;

import com.example.telegrambot.models.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ScheduleRepo extends JpaRepository<Schedule,Long>{
}
