package ru.hemulen.docsigner.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import ru.hemulen.docsigner.model.ErrorMessage;
import ru.hemulen.docsigner.model.OssCorpSimRequest;
import ru.hemulen.docsigner.model.OssCorpSimRequestResponse;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.UUID;

@Service
public class ESIAService {
    private Properties props;
    private String adapterOutPath;
    private String esiaRoutingCode;
    private String mnemonic;
    private String esiaMnemonic;
//TODO: Добавить нормальное логирование
    public ESIAService() {
        try {
            props = new Properties();
            props.load(new FileInputStream("./config/config.ini"));
        } catch (IOException e) {
            System.err.println("Не удалось загрузить конфигурационный файл");
            e.printStackTrace(System.err);
            System.exit(1);
        }
        adapterOutPath = props.getProperty("ADAPTER_OUT_PATH");
        esiaRoutingCode = props.getProperty("ESIA_ROUTING_CODE");
        mnemonic = props.getProperty("IS_MNEMONIC");
        esiaMnemonic = props.getProperty("ESIA_MNEMONIC");

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
        Element rootElement = root.createElementNS("urn://x-artefacts-smev-gov-ru/services/service-adapter/types", "tns:ClientMessage");
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
        Element requestElement = root.createElementNS("urn://mincomsvyaz/esia/oss_corp_sim/1.0.1", "ocs:Request");
        messagePrimaryContent.appendChild(requestElement);
        Element registry = root.createElementNS("urn://x-artefacts-smev-gov-ru/services/message-exchange/types/directive/1.3", "dir:Registry");
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
}