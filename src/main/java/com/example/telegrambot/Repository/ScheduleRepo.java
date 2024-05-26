package com.example.telegrambot.Repository;

import com.example.telegrambot.Database.Database;
import com.example.telegrambot.models.Schedule;


import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;


@Slf4j
public class ScheduleRepo{
    private final Connection conn=Database.getConnection();
    private final String[] days={"monday","tuesday","wednesday","thursday","friday","saturday"};

    public ScheduleRepo() throws SQLException {
    }

    public void addQuery(long chatId, Schedule schedule,int day) {
        try {
            String query = "INSERT INTO schedule (chatid, " + days[day-1] + ") VALUES (?, ?)";

            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            st.setArray(2, conn.createArrayOf("text", schedule.getObjects().toArray()));

            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }


    public void updateQuery(long chatId,Schedule schedule,int day) {
        try {
            String query= "UPDATE schedule SET "+days[day-1]+"=? WHERE chatid=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setArray(1,conn.createArrayOf("text", schedule.getObjects().toArray()) );
            st.setLong(2, chatId);
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public boolean userExist(long chatId){
        try {
            String query = "SELECT * FROM schedule WHERE chatid = ?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public Schedule findById(long chatId, int day) {
        try {
            String query = "SELECT "+days[day-1]+" FROM schedule WHERE chatid = ?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Schedule schedule = new Schedule();
                schedule.setChatID(chatId);

                Array SQL=rs.getArray(days[day-1]);

                if (SQL!=null) {
                    String[] array = (String[]) SQL.getArray();
                    ArrayList<String> objects = new ArrayList<>(Arrays.asList(array));
                    schedule.setObjects(objects);
                    return schedule;
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void addExistQuery(long chatId, Schedule schedule, int day) {
        String week_day=days[day-1];
        try {
            String query= "UPDATE schedule SET "+week_day+"=array_cat("+week_day+",?) WHERE chatid=?";
            PreparedStatement st = conn.prepareStatement(query);

            st.setArray(1,conn.createArrayOf("text", schedule.getObjects().toArray()) );
            st.setLong(2, chatId);
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}
