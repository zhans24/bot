package com.example.telegrambot.Repository;

import com.example.telegrambot.Database.Database;
import com.example.telegrambot.models.Schedule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static java.lang.String.valueOf;

@Slf4j
public class ScheduleRepo{
    private final Connection conn=Database.getConnection();

    public ScheduleRepo() throws SQLException {
    }

    public void addQuery(long chatId, String info) {
        try {
            String query = "INSERT INTO schedule (chatid, day_objects) VALUES (?, ?::json)";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            st.setString(2,info);
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void updateQuery(long chatId,String info) {
        try {
            String query="UPDATE schedule SET day_objects=?::json WHERE chatid=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(2, chatId);
            st.setString(1,info);
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public Schedule findById(long chatId){
        try {
            String query="SELECT schedule WHERE chatid=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            ResultSet rs=st.executeQuery();

            if (rs.next()){
                Schedule schedule=new Schedule();

                schedule.setChatID(rs.getLong("chatid"));
                /*
                 * schedule.setObjects(rs.getObject("", ));
                */
                return schedule;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
