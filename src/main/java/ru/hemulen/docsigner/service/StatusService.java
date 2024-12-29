package ru.hemulen.docsigner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.hemulen.docsigner.config.DocSignerProperties;
import ru.hemulen.docsigner.entity.DBConnection;
import ru.hemulen.docsigner.exception.ResponseParseException;
import ru.hemulen.docsigner.model.MessageResponse;
import ru.hemulen.docsigner.model.StatusResponse;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class StatusService {
    private final DBConnection connection;
    private final String attachmentsInPath;

    @Autowired
    public StatusService(DocSignerProperties properties) {
        attachmentsInPath = properties.getAttachmentInPath();
        this.connection = new DBConnection(properties);
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
                            if (attachments.getString("transfer_method").equals("REFERENCE")) {
                                attachmentPath = attachmentsInPath + "/" +
                                        attachments.getString("id") + "/" +
                                        attachments.getString("message_metadata_id") + "/" +
                                        attachments.getString("file_name");
                            } else {
                                attachmentPath = attachmentsInPath + "/" +
                                        attachments.getString("message_metadata_id") + "/" +
                                        attachments.getString("file_name");
                            }
                            messageResponse.setAttachment(attachmentPath);
                        }
                }
                response.setResponse(messageResponse);
            }
        } catch (SQLException | ResponseParseException e) {
            throw new ResponseParseException("Не удалось сформировать ответ");
        }
        return response;
    }
}
