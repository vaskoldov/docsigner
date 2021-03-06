package ru.hemulen.docsigner.service;

import org.springframework.stereotype.Service;
import ru.hemulen.docsigner.entity.DBConnection;
import ru.hemulen.docsigner.exception.ResponseParseException;
import ru.hemulen.docsigner.model.MessageResponse;
import ru.hemulen.docsigner.model.StatusResponse;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class StatusService {
    private DBConnection connection;

    public StatusService() {
        this.connection = new DBConnection();
    }

    public StatusResponse getStatus(String clientId) throws ResponseParseException {
        StatusResponse response = new StatusResponse();
        response.setRequestMessageId(connection.getMessageId(clientId));
        ResultSet resultSet = connection.getAnswers(clientId);
        try {
            while (resultSet.next()) {
                MessageResponse messageResponse = new MessageResponse();
                messageResponse.setClientId(resultSet.getString("id"));
                messageResponse.setMessageId(resultSet.getString("message_id"));
                messageResponse.setType(resultSet.getString("mode"));
                switch (messageResponse.getType()) {
                    case "STATUS":
                        messageResponse.parseStatus(resultSet.getString("content"));
                        messageResponse.setTimestamp(resultSet.getTimestamp("sending_date"));
                        break;
                    case "REJECT":
                        messageResponse.parseReject(resultSet.getString("content"));
                        messageResponse.setTimestamp(resultSet.getTimestamp("sending_date"));
                        break;
                    case "ERROR":
                        messageResponse.parseError(resultSet.getString("content"));
                        messageResponse.setTimestamp(resultSet.getTimestamp("sending_date"));
                        break;
                    case "MESSAGE":
                        messageResponse.parseMessage(resultSet.getString("content"));
                        ResultSet attachments = connection.getAttachments(messageResponse.getClientId());
                        String attachmentPath;
                        while (attachments.next()) {
                            attachmentPath = "/opt/adapter/data/4.0.3/base-storage/in/" + messageResponse.getClientId() + "/" + attachments.getString("file_name");
                            messageResponse.setAttachment(attachmentPath);
                        }
                }
                response.setResponse(messageResponse);
            }
        } catch (SQLException | ResponseParseException e) {
            throw new ResponseParseException("???? ?????????????? ???????????????????????? ??????????");
        }
        return response;
    }
}
