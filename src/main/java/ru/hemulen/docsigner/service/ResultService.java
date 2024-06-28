package ru.hemulen.docsigner.service;

import org.springframework.stereotype.Service;
import ru.hemulen.docsigner.entity.DBConnection;
import ru.hemulen.docsigner.exception.ResponseParseException;
import ru.hemulen.docsigner.model.MessageResponse;
import ru.hemulen.docsigner.model.ResultResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@Service
public class ResultService {

    private DBConnection connection;
    private String attachmentsInPath;

    ResultService() {

        Properties props = new Properties();
        try {
            props.load(new FileInputStream("./config/config.ini"));
        } catch (IOException e) {
            System.err.println("Не удалось загрузить конфигурационный файл");
            e.printStackTrace(System.err);
            System.exit(1);
        }
        this.attachmentsInPath = props.getProperty("ATTACHMENT_IN_PATH");
        this.connection = new DBConnection();
    }

    public ResultResponse getResult(String clientId) throws ResponseParseException {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setResult("IN_PROCESS");
        // Получаем все ответы на запрос с clientId
        ResultSet resultSet = this.connection.getAnswers(clientId);
        try {
            while (resultSet.next()) {
                MessageResponse messageResponse = new MessageResponse();
                messageResponse.setType(resultSet.getString("mode"));
                switch (messageResponse.getType()) {
                    case "STATUS":
                        // Уже установлен результат "В процессе". Статус на него не влияет.
                        break;
                    case "REJECT":
                    case "ERROR":
                        resultResponse.setResult("COMPLETED");
                        break;
                    case "MESSAGE":
                        resultResponse.setResult("COMPLETED");
                        ResultSet attachmentsSet = this.connection.getAttachments(resultSet.getString("id"));
                        while (attachmentsSet.next()) {
                            if (attachmentsSet.getString("transfer_method").equals("REFERENCE")) {
                                // Для вложений, пришедших через FTP, первый подкаталог - это id из AttachmentHeader.
                                // Он же (по утверждению разработчиков) хранится в поле id таблицы attachment_metadata
                                resultResponse.addAttachment(attachmentsInPath + "/" +
                                        attachmentsSet.getString("id") + "/" +
                                        attachmentsSet.getString("message_metadata_id") + "/" +
                                        attachmentsSet.getString("file_name"));
                            } else {
                                resultResponse.addAttachment(attachmentsInPath + "/" +
                                        attachmentsSet.getString("message_metadata_id") + "/" +
                                        attachmentsSet.getString("file_name"));
                            }
                        }
                        break;
                }
            }
        } catch (SQLException e) {
            throw new ResponseParseException("Не удалось сформировать ответ");
        }
        return resultResponse;
    }
}
