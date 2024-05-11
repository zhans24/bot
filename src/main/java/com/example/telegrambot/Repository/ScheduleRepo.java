package com.example.telegrambot.Repository;

import com.example.telegrambot.Database.Database;
import com.fasterxml.jackson.core.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
@Slf4j
public class ScheduleRepo{
    private final Connection conn=Database.getConnection();

    public ScheduleRepo() throws SQLException {
    }

    public void addQuery(long chatId, JsonParser info) {
        try {
            String query = "INSERT INTO schedule (chatid, day_objects) VALUES (?, ?)";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            st.setString(2, String.valueOf(info));
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}
