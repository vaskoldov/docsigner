package ru.hemulen.docsigner.model;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.hemulen.docsigner.exception.ResponseParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;

public class MessageResponse {
    private String clientId;
    private String messageId;
    private String type;
    private String code;
    private String description;
    private String timestamp;
    private Boolean signRejected;
    private ArrayList<String> attachments = new ArrayList<>();
    private Document xmlDocument;
    private DocumentBuilderFactory factory;

    public MessageResponse() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        attachments = new ArrayList<>();
    }

    public void parseStatus(String xml) throws ResponseParseException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression;
        String query;
        try {
            xmlDocument = factory.newDocumentBuilder().parse(is);
            query = "/*[local-name()='Status']/*[local-name()='Code']";
            xPathExpression = xPath.compile(query);
            code = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
            query = "/*[local-name()='Status']/*[local-name()='Description']";
            xPathExpression = xPath.compile(query);
            description = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
        } catch (ParserConfigurationException | XPathExpressionException | IOException | SAXException e) {
            throw new ResponseParseException(String.format("Ошибка при чтении ответа c cleintId %s из XML", clientId));
        }
    }

    public void parseReject(String xml) throws ResponseParseException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression;
        String query;
        try {
            xmlDocument = factory.newDocumentBuilder().parse(is);
            query = "/*[local-name()='Reject']/*[local-name()='Item']/*[local-name()='Code']";
            xPathExpression = xPath.compile(query);
            code = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
            query = "/*[local-name()='Reject']/*[local-name()='Item']/*[local-name()='Description']";
            xPathExpression = xPath.compile(query);
            description = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
        } catch (ParserConfigurationException | XPathExpressionException | IOException | SAXException e) {
            throw new ResponseParseException(String.format("Ошибка при чтении ответа c cleintId %s из XML", clientId));
        }
    }
    public void parseError(String xml) throws ResponseParseException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression;
        String query;
        try {
            xmlDocument = factory.newDocumentBuilder().parse(is);
            query = "/*[local-name()='Error']/*[local-name()='Code']";
            xPathExpression = xPath.compile(query);
            code = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
            query = "/*[local-name()='Error']/*[local-name()='Description']";
            xPathExpression = xPath.compile(query);
            description = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
        } catch (ParserConfigurationException | XPathExpressionException | IOException | SAXException e) {
            throw new ResponseParseException(String.format("Ошибка при чтении ответа c cleintId %s из XML", clientId));
        }
    }

    public void parseMessage(String xml) throws ResponseParseException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression;
        String query;
        try {
            xmlDocument = factory.newDocumentBuilder().parse(is);
            query = "/*[local-name()='ResponseSigContract']/@timestamp";
            xPathExpression = xPath.compile(query);
            timestamp = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
            query = "//*[local-name()='SignReject']";
            xPathExpression = xPath.compile(query);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.NODESET);
            if (nodeList.getLength() > 0) {
                signRejected = true;
            }
            query = "/*[local-name()='ResponseSigContract']/*[local-name()='Error']/*[local-name()='ErrorCode']/text()";
            xPathExpression = xPath.compile(query);
            code = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
            query = "/*[local-name()='ResponseSigContract']/*[local-name()='Error']/*[local-name()='ErrorMessage']/text()";
            xPathExpression = xPath.compile(query);
            description = (String) xPathExpression.evaluate(xmlDocument.getDocumentElement(), XPathConstants.STRING);
        } catch (ParserConfigurationException | XPathExpressionException | IOException | SAXException e) {
            throw new ResponseParseException(String.format("Ошибка при чтении ответа c cleintId %s из XML", clientId));
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAttachment(String attachment) {
        this.attachments.add(attachment);
    }

    public ArrayList<String> getAttachments(){
        return attachments;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        if (timestamp != null) {
            this.timestamp = timestamp.toString();
        } else {
            this.timestamp = null;
        }
    }

    public Boolean getSignRejected() {
        return signRejected;
    }

    public void setSignRejected(Boolean signRejected) {
        this.signRejected = signRejected;
    }


}
