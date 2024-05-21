package com.example.telegrambot.Repository;

import com.example.telegrambot.Database.Database;
import com.example.telegrambot.models.Schedule;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class ScheduleRepo{
    private final Connection conn=Database.getConnection();

    public ScheduleRepo() throws SQLException {
    }

    public void addQuery(long chatId, Schedule schedule) {
        try {
            String query = "INSERT INTO schedule (chatid, days,objects) VALUES (?, ?, ?)";

            PreparedStatement st = conn.prepareStatement(query);

            st.setLong(1, chatId);
            st.setArray(2,conn.createArrayOf("smallint",schedule.getDays().toArray()));
            st.setArray(3,conn.createArrayOf("text", schedule.getObjects().toArray()));

            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void updateQuery(long chatId,Schedule schedule) {
        try {
            String query= """ 
                    UPDATE schedule 
                    SET days=?, objects=?
                    WHERE chatid=?
                    """;
            PreparedStatement st = conn.prepareStatement(query);
            st.setArray(1, conn.createArrayOf("smallint",schedule.getDays().toArray() ) );
            st.setArray(2,conn.createArrayOf("text", schedule.getObjects().toArray()) );
            st.setLong(3, chatId);
            st.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public Schedule findById(long chatId, int day) {
        try {
            String query = "SELECT days, objects FROM schedule WHERE chatid = ?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setLong(1, chatId);
            ResultSet rs = st.executeQuery();

            if (rs.next() && getDay(chatId, day) != 0) {
                Schedule schedule = new Schedule();
                schedule.setChatID(chatId);

                Array arraySQL = rs.getArray("days");

                // Convert the object to an array of strings
                Integer[] days = (Integer[]) arraySQL.getArray();
                schedule.setDays(new ArrayList<>(Arrays.asList(days)));

                arraySQL = rs.getArray("objects");
                Object[] array = (Object[]) arraySQL.getArray();

                // Create a list to hold the objects
                ArrayList<ArrayList<String>> objects = new ArrayList<>();

                // Iterate over each object in the array
                for (Object o : array) {
                    // Convert the object to an array of strings
                    String[] nestedArray = (String[]) ((Array) o).getArray();
                    // Add the array of strings to the list of objects
                    objects.add(new ArrayList<>(Arrays.asList(nestedArray)));
                }

                schedule.setObjects(objects);

                return schedule;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }


    private int getDay(long chatId,int day){
        try {
            String query="SELECT days[?] FROM schedule WHERE chatid = ?";
            PreparedStatement st=conn.prepareStatement(query);

            st.setInt(1, day);
            st.setLong(2, chatId);

            ResultSet rs=st.executeQuery();
            if (rs.next())
                return rs.getInt("days");

        }catch (SQLException e){
            log.error(e.getMessage());
        }
        return 0;
    }

}
