package com.example.telegrambot.models;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Table(name="Schedule")
public class Schedule {
    @Id
    private long chatID;
    private List<String> objects;

}
