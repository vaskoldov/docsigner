package ru.hemulen.docsigner.entity;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DBConnection implements AutoCloseable {
    private Connection connection;
    public DBConnection() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("./config/config.ini"));
        } catch (IOException e) {
            System.err.println("Не удалось загрузить конфигурационный файл");
            e.printStackTrace(System.err);
            System.exit(1);
        }
        String dbURL = props.getProperty("DB_URL");
        String dbUser = props.getProperty("DB_USER");
        String dbPass = props.getProperty("DB_PASS");
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(dbURL, dbUser, dbPass);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
    public void close()  {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
    public ResultSet getAnswers(String clientId) {
        try {
            String sql = String.format("select mm.id, mm.message_id , mm.sending_date, mc.\"mode\" , mc.\"content\" \n" +
                    "from core.message_metadata mm \n" +
                    "inner join core.message_content mc on mc.id = mm.id \n" +
                    "where mm.reference_id = '%s' order by mm.creation_date;", clientId);
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public String getMessageId(String clientId) {
        try {
            String sql = String.format("select message_id from \"core\".message_metadata where id = '%s'", clientId);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet != null && resultSet.next()) {
                return resultSet.getString("message_id");
            }
            return null;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public ResultSet getAttachments(String clientId) {
        try {
            String sql = String.format("select id, message_metadata_id, file_name, transfer_method from \"core\".attachment_metadata where message_metadata_id = '%s'", clientId);
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
