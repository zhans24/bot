package com.example.telegrambot.models;


import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;

@Data
@Table(name="Schedule")
public class Schedule {
    @Id
    private long chatID;
    private ArrayList<Integer> days;
    private ArrayList<ArrayList<String>> objects;

}
