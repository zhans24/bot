package com.example.telegrambot.Database;

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    public static Connection getConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl("jdbc:postgresql://avian-gundi-15339.8nj.gcp-europe-west1.cockroachlabs.cloud:26257/botdb");
        return ds.getConnection("zhanserik","lFiq4MtPkAqRqjVTyw_0eg");
    }
}

