package ru.hemulen.docsigner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.hemulen.docsigner.config.DocSignerProperties;
import ru.hemulen.docsigner.entity.DBConnection;
import ru.hemulen.docsigner.exception.ResponseParseException;
import ru.hemulen.docsigner.model.MessageResponse;
import ru.hemulen.docsigner.model.ResultResponse;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class ResultService {

    private final DBConnection connection;
    private final String attachmentsInPath;

    @Autowired
    ResultService(DocSignerProperties properties) {
        this.attachmentsInPath = properties.getAttachmentInPath();
        this.connection = new DBConnection(properties);
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
