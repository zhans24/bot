package com.example.telegrambot.models;


import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Table(name="Schedule")
public class Schedule {
    @Id
    private long chatID;
    private String day;
    private String object;
    private Timestamp time;
    private String classroom;
}
