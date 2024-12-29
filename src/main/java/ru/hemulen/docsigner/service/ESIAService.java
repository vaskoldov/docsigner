package ru.hemulen.docsigner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.hemulen.docsigner.config.DocSignerProperties;
import ru.hemulen.docsigner.entity.DBConnection;
import ru.hemulen.docsigner.model.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

@Service
public class ESIAService {
    private static final String ESIA_NAMESPACE = "urn://mincomsvyaz/esia/oss_corp_sim/1.0.1";
    private static final String ADAPTER_NAMESPACE = "urn://x-artefacts-smev-gov-ru/services/service-adapter/types";
    private static final String DIRECTIVE_NAMESPACE = "urn://x-artefacts-smev-gov-ru/services/message-exchange/types/directive/1.3";

    private Properties props;
    private DBConnection connection;
    private String adapterOutPath;
    private String esiaRoutingCode;
    private String mnemonic;
    private String esiaMnemonic;
//TODO: Добавить нормальное логирование
    @Autowired
    public ESIAService(DocSignerProperties properties) {
        adapterOutPath = properties.getAdapterOutPath();
        esiaRoutingCode = properties.getEsiaRoutingCode();
        mnemonic = properties.getIsMnemonic();
        esiaMnemonic = properties.getEsiaMnemonic();
        connection = new DBConnection(properties);

    }
    public ResponseEntity processOssCorpSimRequest(OssCorpSimRequest[] request) throws ParserConfigurationException {
        // Выполняем ФЛК полученного параметра
        if (! checkParameters(request)) {
            return (ResponseEntity) ResponseEntity.badRequest().body(new ErrorMessage("400", "Синтаксическая ошибка в запросе"));
        }
        // Формируем clientID для запроса
        String clientId = UUID.randomUUID().toString();
        // Создаем оболочку ClientMessage
        org.w3c.dom.Document root = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = root.createElementNS(ADAPTER_NAMESPACE, "tns:ClientMessage");
        root.appendChild(rootElement);
        Element itSystem = root.createElement("tns:itSystem");
        itSystem.appendChild(root.createTextNode(mnemonic));
        rootElement.appendChild(itSystem);

        // RequestMessage
        Element requestMessage = root.createElement("tns:RequestMessage");
        rootElement.appendChild(requestMessage);

        // RequestMetadata
        Element requestMetadata = root.createElement("tns:RequestMetadata");
        requestMessage.appendChild(requestMetadata);
        Element clientIdElement = root.createElement("tns:clientId");
        clientIdElement.appendChild(root.createTextNode(clientId));
        requestMetadata.appendChild(clientIdElement);

        // RoutingInformation
        Element routingInformation  = root.createElement("tns:RoutingInformation");
        requestMetadata.appendChild(routingInformation);
        Element registryRouting = root.createElement("tns:RegistryRouting");
        routingInformation.appendChild(registryRouting);
        for (int i = 0; i < request.length; i++) {
            Element registryRecordRouting = root.createElement("tns:RegistryRecordRouting");
            registryRouting.appendChild(registryRecordRouting);
            Element recordId = root.createElement("tns:RecordId");
            recordId.appendChild(root.createTextNode(Integer.toString(i + 1)));
            registryRecordRouting.appendChild(recordId);
            Element useGeneralRouting = root.createElement("tns:UseGeneralRouting");
            useGeneralRouting.appendChild(root.createTextNode("false"));
            registryRecordRouting.appendChild(useGeneralRouting);
            Element dynamicRouting = root.createElement("tns:DynamicRouting");
            registryRecordRouting.appendChild(dynamicRouting);
            Element dynamicValue = root.createElement("tns:DynamicValue");
            dynamicValue.appendChild(root.createTextNode(esiaMnemonic));
            dynamicRouting.appendChild(dynamicValue);
        }

        // RequestContent
        Element requestContent = root.createElement("tns:RequestContent");
        requestMessage.appendChild(requestContent);

        // content
        Element content = root.createElement("tns:content");
        requestContent.appendChild(content);

        // MessagePrimaryContent
        Element messagePrimaryContent = root.createElement("tns:MessagePrimaryContent");
        content.appendChild(messagePrimaryContent);

        // Формируем запрос по виду сведений
        Element requestElement = root.createElementNS(ESIA_NAMESPACE, "ocs:Request");
        messagePrimaryContent.appendChild(requestElement);
        Element registry = root.createElementNS(DIRECTIVE_NAMESPACE, "dir:Registry");
        requestElement.appendChild(registry);
        // Последовательно обрабатываем каждый элемент массива в параметре метода и добавляем его в Registry
        for (int i = 0; i < request.length; i++) {
            OssCorpSimRequest currentRecord = request[i];
            Element registryRecord = root.createElement("dir:RegistryRecord");
            registry.appendChild(registryRecord);
            Element recordId = root.createElement("dir:RecordId");
            recordId.appendChild(root.createTextNode(Integer.toString(i + 1)));
            registryRecord.appendChild(recordId);
            Element record = root.createElement("dir:Record");
            registryRecord.appendChild(record);
            Element recordContent = root.createElement("dir:RecordContent");
            record.appendChild(recordContent);
            Element claimOSSRequest = root.createElement("ocs:ClaimOSSRequest");
            recordContent.appendChild(claimOSSRequest);
            Element routingCodeElement = root.createElement("ocs:RoutingCode");
            routingCodeElement.appendChild(root.createTextNode(esiaRoutingCode));
            claimOSSRequest.appendChild(routingCodeElement);
            Element claimNum = root.createElement("ocs:ClaimNum");
            claimNum.appendChild(root.createTextNode(currentRecord.getClaimNum()));
            claimOSSRequest.appendChild(claimNum);
            // Ветвление на M2M и телефон физического лица
            if (currentRecord.getM2m() != null) {
                // Обрабатываем M2M ветку
                Element m2m = root.createElement("ocs:M2m");
                claimOSSRequest.appendChild(m2m);
                Element inn = root.createElement("ocs:Inn");
                inn.appendChild(root.createTextNode(currentRecord.getM2m().getInn()));
                m2m.appendChild(inn);
            } else {
                Element dul = root.createElement("ocs:Dul");
                claimOSSRequest.appendChild(dul);
                Element docIssOn = root.createElement("ocs:DocIssOn");
                docIssOn.appendChild(root.createTextNode(currentRecord.getDul().getDocIssOn()));
                dul.appendChild(docIssOn);
                Element docNum = root.createElement("ocs:DocNum");
                docNum.appendChild(root.createTextNode(currentRecord.getDul().getDocNum()));
                dul.appendChild(docNum);
                Element brd = root.createElement("ocs:Brd");
                brd.appendChild(root.createTextNode(currentRecord.getDul().getBrd()));
                dul.appendChild(brd);
                if (currentRecord.getDul().getDocType() != null) {
                    Element docTyp = root.createElement("ocs:DocTyp");
                    docTyp.appendChild(root.createTextNode(currentRecord.getDul().getDocType()));
                    dul.appendChild(docTyp);
                }
                if (currentRecord.getDul().getDocSer() != null) {
                    Element docSer = root.createElement("ocs:DocSer");
                    docSer.appendChild(root.createTextNode(currentRecord.getDul().getDocSer()));
                    dul.appendChild(docSer);
                }
                if (currentRecord.getPhoneNumber10() != null) {
                    Element phoneNumber10 = root.createElement("ocs:phoneNumber10");
                    phoneNumber10.appendChild(root.createTextNode(currentRecord.getPhoneNumber10()));
                    claimOSSRequest.appendChild(phoneNumber10);
                } else if (currentRecord.getPhoneNumber14() != null) {
                    Element phoneNumber14 = root.createElement("ocs:phoneNumber14");
                    phoneNumber14.appendChild(root.createTextNode(currentRecord.getPhoneNumber14()));
                    claimOSSRequest.appendChild(phoneNumber14);
                } else if (currentRecord.getUki() != null) {
                    Element uki = root.createElement("ocs:uki");
                    uki.appendChild(root.createTextNode(currentRecord.getUki()));
                    claimOSSRequest.appendChild(uki);
                }
            }
        }

        // Сохраняем XML в файл с именем clientId в каталог adapterOutPath
        Path outPath = Paths.get(adapterOutPath, clientId + ".xml");
        try {
            FileWriter fileWriter = new FileWriter(outPath.toFile());
            DOMSource domSource = new DOMSource(root);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            fileWriter.write(writer.toString());
            fileWriter.close();
        } catch (IOException | TransformerException e) {
            return (ResponseEntity) ResponseEntity.internalServerError().body(new ErrorMessage("500", e.getMessage()));
        }
        OssCorpSimRequestResponse response = new OssCorpSimRequestResponse(clientId);
        return (ResponseEntity) ResponseEntity.ok().body(response);
    }

    private boolean checkParameters(OssCorpSimRequest[] request) {
        // Проверка на непустой массив
        if (request.length == 0) {
            return false;
        }
        // Проверка обязательных элементов
        for (int i = 0; i < request.length; i++) {
            OssCorpSimRequest current = request[i];
            if (current.getClaimNum() == null) return false;
            if (current.getM2m() != null) {
                if (current.getM2m().getInn() == null) return false;
            } else if (current.getDul() != null) {
                if (current.getDul().getDocIssOn() == null) return false;
                if (current.getDul().getDocNum() == null) return false;
                if (current.getDul().getBrd() == null) return false;
                if (current.getPhoneNumber10() == null && current.getPhoneNumber14() == null && current.getUki() == null) return false;
                // Проверяем форматы дат
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    dateFormat.parse(current.getDul().getBrd());
                    dateFormat.parse(current.getDul().getDocIssOn());
                } catch (ParseException e) {
                    return false;
                }
                // Проверяем значение DocTyp, если он передан (должно быть одно из значений enumeration из схемы)
                if (current.getDul().getDocType() != null) {
                    if ( ! ("RF_PASSPORT".equals(current.getDul().getDocType()) ||
                            "FRGN_PASS".equals(current.getDul().getDocType()) ||
                            "FID_DOC".equals(current.getDul().getDocType())))
                        return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public ResponseEntity<OssCorpSimResponse> processOssCorpSimResponse(String clientId) {
        // С clientId идем в базу адаптера и получаем ответы на этот запрос
        ResultSet resultSet = connection.getAnswers(clientId);
        // Перебираем ответы и ищем бизнес-ответ ЕСИА по сути запроса
        try {
            while (resultSet.next()) {
                String messageType = resultSet.getString("mode");
                switch (messageType) {
                    case "STATUS":
                        continue;
                    case "ERROR":
                    case "REJECT":
                        // TODO:Обработать ошибку
                        break;
                    case "MESSAGE":
                        // Вот тут парсим ответ и возвращаем в ответ на Get
                        String responseBody = resultSet.getString("content");
                        String responseId = resultSet.getString("id");
                        OssCorpSimResponse response = getClaimOssResponse(responseBody, responseId);
                        return (ResponseEntity) ResponseEntity.ok(response);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Метод парсит ответ ЕСИА и создает из него DTO ClaimOssResponse
     * @param response Строка с XML ответа
     * @return DTO-объект ClaimOssResponse
     */
    private OssCorpSimResponse getClaimOssResponse(String response, String responseId) {
        OssCorpSimResponse ossCorpSimResponse = new OssCorpSimResponse();
        ossCorpSimResponse.setResponseClientId(responseId);
        ArrayList<ClaimOssResponse> claimOssResponseList = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document document = documentBuilder.parse(response);
            Element root = document.getDocumentElement();
            // Получаем список элементов RegistryRecord, обрабатываем и заполняем массив claimOssResponseList
            NodeList registryRecordList = root.getElementsByTagNameNS(DIRECTIVE_NAMESPACE, "RegistryRecord");
            for (int i = 0; i < registryRecordList.getLength(); i++) {
                Element registryRecord = (Element) registryRecordList.item(i);
                ClaimOssResponse claimOssResponse = new ClaimOssResponse();
                claimOssResponse.setRecordID(registryRecord.getElementsByTagNameNS(DIRECTIVE_NAMESPACE, "RecordId").item(0).getTextContent());
                claimOssResponse.setStatus(registryRecord.getElementsByTagNameNS(ESIA_NAMESPACE, "status").item(0).getTextContent());
                ArrayList<Resp> respList = new ArrayList<>();
                // Внутри каждой RegistryRecord ищем все элементы Resp, обрабатываем и заполняем массив respList
                NodeList respNodeList = registryRecord.getElementsByTagNameNS(ESIA_NAMESPACE, "Resp");
                for (int j = 0; j < respNodeList.getLength(); j++) {
                    Element respElement = (Element) respNodeList.item(j);
                    Resp resp = new Resp();
                    NodeList ossSimConfirmNodeList = respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "OssSimConfirm");
                    if (ossSimConfirmNodeList.getLength() > 0) {
                        resp.setOssSimConfirm(ossSimConfirmNodeList.item(0).getTextContent());
                    }
                    resp.setOssNam(respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "OssNam").item(0).getTextContent());
                    NodeList innNodeList = respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "Inn");
                    if (innNodeList.getLength() > 0) {
                        resp.setInn(innNodeList.item(0).getTextContent());
                    }
                    NodeList phoneNumber10NodeList = respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "phoneNumber10");
                    if (phoneNumber10NodeList.getLength() > 0) {
                        resp.setPhoneNumber10(phoneNumber10NodeList.item(0).getTextContent());
                    }
                    NodeList phoneNumber14NodeList = respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "phoneNumber14");
                    if (phoneNumber14NodeList.getLength() > 0) {
                        resp.setPhoneNumber14(phoneNumber14NodeList.item(0).getTextContent());
                    }
                    NodeList ukiNodeList = respElement.getElementsByTagNameNS(ESIA_NAMESPACE, "uki");
                    if (ukiNodeList.getLength() > 0) {
                        resp.setUki(ukiNodeList.item(0).getTextContent());
                    }
                    respList.add(resp);
                }
                claimOssResponseList.add(claimOssResponse);
            }
            ossCorpSimResponse.setClaimOssResponse(claimOssResponseList);
            return ossCorpSimResponse;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            //TODO: Вернуть код 500 и описание ошибки
            throw new RuntimeException(e);
        }
    }
}
