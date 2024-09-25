package ru.hemulen.docsigner.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import ru.hemulen.crypto.exceptions.SignatureProcessingException;
import ru.hemulen.docsigner.exception.*;
import ru.hemulen.docsigner.model.Document;
import ru.hemulen.docsigner.model.OssCorpSimRequest;
import ru.hemulen.signer.Signer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DocumentService {
    Signer signer;
    String containerAlias;
    String containerPassword;
    String adapterOutPath;
    String attachmentOutPath;
    String backLinkURL;
    Base64.Decoder decoder;
    Base64.Encoder encoder;

    public DocumentService()  {
        //TODO:Заменить config.ini на application.properties
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("./config/config.ini"));
        } catch (IOException e) {
            System.err.println("Не удалось загрузить конфигурационный файл");
            e.printStackTrace(System.err);
            System.exit(1);
        }
        containerAlias = props.getProperty("CONTAINER_ALIAS");
        containerPassword = props.getProperty("CONTAINER_PASSWORD");
        adapterOutPath = props.getProperty("ADAPTER_OUT_PATH");
        attachmentOutPath = props.getProperty("ATTACHMENT_OUT_PATH");
        backLinkURL = props.getProperty("BACK_LINK");
        try {
            signer = new Signer(containerAlias, containerPassword);

        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {}
        decoder = Base64.getDecoder();
        encoder = Base64.getEncoder();
    }

    public String processDocument(Document document) throws DocumentSignException, DocumentFileNotExists, XMLTransformationException, FileOperationsException, IncorrectParameterException {
        document.setClientId(UUID.randomUUID().toString());
        // Проверяем наличие файла с документом в каталоге
        File documentFile = new File(document.getDocumentPath());
        if (!documentFile.exists()) {
            // Если файл не сохранен перед вызовом метода, то записываем в него содержимое элемента documentContent
            String documentContent = document.getDocumentContent();
            if (documentContent == null || documentContent.length() == 0) {
                throw new DocumentFileNotExists("Отсутствует содержимое файла в параметре documentContent");
            }
            try {
                FileOutputStream fos = new FileOutputStream(document.getDocumentPath());
                InputStream is = decoder.wrap(new ByteArrayInputStream(documentContent.getBytes(StandardCharsets.UTF_8)));
                int _byte;
                while ((_byte = is.read()) != -1) {
                    fos.write(_byte);
                }
                fos.close();
            } catch (IOException e) {
                throw new FileOperationsException("Не удалось сохранить содержимое документа в файл.");
            }
        }
        // Проверяем наличие параметра signExp
        if (document.getSignExp() == null) {
            // Увеличиваем текущее время на 24 часа
            try {
                document.setSignExp(getExpireTimestamp(getCurrentTimestamp()));
            } catch (ParseException e) {
                throw new IncorrectParameterException();
            }
        }
        // Проверяем корректность даты signExp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            Date current = dateFormat.parse(document.getSignExp());
        } catch (ParseException e) {
            throw new IncorrectParameterException("Некорректный форматы даты signExp! Допустимый формат \"yyyy-MM-dd'T'HH:mm:ss\".");
        }
        // Подписываем документ
        File signFile = signDocument(document);
        // Кодируем подпись в base64 и возвращаем в ответе
        document.setSignContent(getFileContentEncoded(signFile));
        // Формируем XML c запросом
        String clientMessage = createClientMessage(document);
        saveClientMessage(document, clientMessage);
        return  document.getClientId();
    }

    public String processDocumentUKEP(Document document) throws DocumentSignException, DocumentFileNotExists, XMLTransformationException, FileOperationsException, IncorrectParameterException {
        document.setClientId(UUID.randomUUID().toString());
        // Проверяем наличие файла с документом в каталоге
        File documentFile = new File(document.getDocumentPath());
        if (!documentFile.exists()) {
            // Если файл не сохранен перед вызовом метода, то записываем в него содержимое элемента documentContent
            String documentContent = document.getDocumentContent();
            if (documentContent == null || documentContent.length() == 0) {
                throw new DocumentFileNotExists("Отсутствует содержимое файла в параметре documentContent");
            }
            try {
                FileOutputStream fos = new FileOutputStream(document.getDocumentPath());
                InputStream is = decoder.wrap(new ByteArrayInputStream(documentContent.getBytes(StandardCharsets.UTF_8)));
                int _byte;
                while ((_byte = is.read()) != -1) {
                    fos.write(_byte);
                }
                fos.close();
            } catch (IOException e) {
                throw new FileOperationsException("Не удалось сохранить содержимое документа в файл.");
            }
        }
        // Проверяем наличие параметра signExp
        if (document.getSignExp() == null) {
            // Увеличиваем текущее время на 24 часа
            try {
                document.setSignExp(getExpireTimestamp(getCurrentTimestamp()));
            } catch (ParseException e) {
                throw new IncorrectParameterException();
            }
        }
        // Проверяем корректность даты signExp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            Date current = dateFormat.parse(document.getSignExp());
        } catch (ParseException e) {
            throw new IncorrectParameterException();
        }
        // Подписываем документ
        File signFile = signDocument(document);
        // Формируем XML c запросом
        String clientMessage = createClientMessageUKEP(document);
        saveClientMessage(document, clientMessage);
        return document.getClientId();
    }

    private File signDocument (Document document) throws DocumentFileNotExists, DocumentSignException {
        File documentFile = Paths.get(document.getDocumentPath()).toFile();
        if (!documentFile.exists()) {
            throw new DocumentFileNotExists("Указанный файл c документом не найден");
        }
        try {
            // Копируем подписываемый файл в каталог ./signed/[OID]
            Path targetPath = Paths.get(documentFile.toPath().getParent().toString(), "signed", document.getClientId());
            File targetFile = Paths.get(targetPath.toString(), documentFile.getName()).toFile();
            if (!targetPath.getParent().toFile().exists()) {
                Files.createDirectory(targetPath.getParent());
            }
            if (!targetPath.toFile().exists()) {
                Files.createDirectory(targetPath);
            }
            Files.move(documentFile.toPath(), targetFile.toPath());
            documentFile = targetFile;
            // Обновляем местоположение файла в документе тоже
            // document.setDocumentPath(documentFile.getPath());
            File docSign = signer.signPKCS7Detached(documentFile);
            // Копируем подписанный файл и подпись в local-storage
            Path attachmentPath = Paths.get(attachmentOutPath, document.getClientId());
            Files.createDirectory(attachmentPath);
            Path attachmentFile = attachmentPath.resolve(documentFile.getName());
            Path attachmentSign = attachmentPath.resolve(docSign.getName());
            Files.copy(documentFile.toPath(), attachmentFile);
            Files.copy(docSign.toPath(), attachmentSign);
            document.setDocumentPath(attachmentFile.toString());
            return attachmentSign.toFile();
        } catch (SignatureProcessingException | IOException e) {
            throw new DocumentSignException("Не удалось подписать документ");
        }
    }

    public void saveClientMessage(Document document, String clientMessage) throws FileOperationsException {
        Path outPath = Paths.get(adapterOutPath, document.getClientId() + ".xml");
        FileWriter fw = null;
        try {
            fw = new FileWriter(outPath.toFile());
            fw.write(clientMessage);
            fw.close();
        } catch (IOException e) {
            throw new FileOperationsException("Не удалось сохранить xml-файл с запросом");
        }
    }

    private String createClientMessage(Document document) throws XMLTransformationException {
        try {
            // Определяем переменные, которые будут использоваться в запросе
            String clientIdValue = document.getClientId();
            String contractUUID = UUID.randomUUID().toString();
            String signUUID = UUID.randomUUID().toString();
            String currentTime = getCurrentTimestamp();
            //String expireTime = getExpireTimestamp(currentTime);
            // ========= Заголовок ClientMessage и метаданные запроса =========
            org.w3c.dom.Document root = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element rootElement = root.createElementNS("urn://x-artefacts-smev-gov-ru/services/service-adapter/types", "tns:ClientMessage");
            root.appendChild(rootElement);
            Element itSystem = root.createElement("tns:itSystem");
            itSystem.appendChild(root.createTextNode(document.getMnemonic()));
            rootElement.appendChild(itSystem);
            Element requestMessage = root.createElement("tns:RequestMessage");
            rootElement.appendChild(requestMessage);
            Element requestMetadata = root.createElement("tns:RequestMetadata");
            requestMessage.appendChild(requestMetadata);
            Element clientId = root.createElement("tns:clientId");
            clientId.appendChild(root.createTextNode(clientIdValue));
            requestMetadata.appendChild(clientId);
            Element requestContent = root.createElement("tns:RequestContent");
            requestMessage.appendChild(requestContent);
            Element content = root.createElement("tns:content");
            requestContent.appendChild(content);
            Element messagePrimaryContent = root.createElement("tns:MessagePrimaryContent");
            content.appendChild(messagePrimaryContent);
            // ======= Бизнес-запрос вида сведений =======
            Element requestSigContract = root.createElementNS("urn://gosuslugi/sig-contract/1.0.2", "sig:RequestSigContract");
            requestSigContract.setAttribute("Id", "ID_" + clientIdValue);
            requestSigContract.setAttribute("timestamp", currentTime);
            requestSigContract.setAttribute("routeNumber", "MNSV03");
            messagePrimaryContent.appendChild(requestSigContract);
            Element OID = root.createElement("sig:OID");
            OID.appendChild(root.createTextNode(document.getOid()));
            requestSigContract.appendChild(OID);
            Element signExp = root.createElement("sig:signExp");
            //signExp.appendChild(root.createTextNode(expireTime)); -- предельное время подписания документа теперь передается в запросе
            signExp.appendChild(root.createTextNode(document.getSignExp()));
            requestSigContract.appendChild(signExp);
            Element descDoc = root.createElement("sig:descDoc");
            descDoc.appendChild(root.createTextNode(document.getDescDoc()));
            requestSigContract.appendChild(descDoc);
            Element contracts = root.createElement("sig:Contracts");
            requestSigContract.appendChild(contracts);
            Element contract = root.createElement("sig:Contract");
            contracts.appendChild(contract);
            Element docElement = root.createElement("sig:Document");
            docElement.setAttribute("docId", document.getDocumentId());
            docElement.setAttribute("mimeType", "application/pdf");
            docElement.setAttribute("uuid", contractUUID);
            docElement.setAttribute("description", document.getDocumentDescription());
            contract.appendChild(docElement);
            Element signElement = root.createElement("sig:Signature");
            signElement.setAttribute("docId", "Подпись оператора");
            signElement.setAttribute("mimeType", "application/sig");
            signElement.setAttribute("uuid", signUUID);
            contract.appendChild(signElement);
            Element backlink = root.createElement("sig:Backlink");
            backlink.appendChild(root.createTextNode(backLinkURL));
            requestSigContract.appendChild(backlink);
            // ========== Блок вложений ClientMessage =========
            Element attachmentHeaderList = root.createElement("tns:AttachmentHeaderList");
            content.appendChild(attachmentHeaderList);
            Element docAttachmentHeader = root.createElement("tns:AttachmentHeader");
            attachmentHeaderList.appendChild(docAttachmentHeader);
            Element contractId = root.createElement("tns:Id");
            contractId.appendChild(root.createTextNode(contractUUID));
            docAttachmentHeader.appendChild(contractId);
            Element docPath = root.createElement("tns:filePath");
            docPath.appendChild(root.createTextNode(document.getDocumentPath()));
            docAttachmentHeader.appendChild(docPath);
            Element docTransferMethod = root.createElement("tns:TransferMethod");
            docTransferMethod.appendChild(root.createTextNode("REFERENCE"));
            docAttachmentHeader.appendChild(docTransferMethod);
            Element signAttachmentHeader = root.createElement("tns:AttachmentHeader");
            attachmentHeaderList.appendChild(signAttachmentHeader);
            Element signId = root.createElement("tns:Id");
            signId.appendChild(root.createTextNode(signUUID));
            signAttachmentHeader.appendChild(signId);
            Element signPath = root.createElement("tns:filePath");
            signPath.appendChild(root.createTextNode(document.getDocumentPath() + ".sig"));
            signAttachmentHeader.appendChild(signPath);
            Element signTransferMethod = root.createElement("tns:TransferMethod");
            signTransferMethod.appendChild(root.createTextNode("REFERENCE"));
            signAttachmentHeader.appendChild(signTransferMethod);
            return getStringFromDocument(root);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new XMLTransformationException("Не удалось сформировать XML с запросом");
        }
    }

    private String createClientMessageUKEP(Document document) throws XMLTransformationException {
        try {
            // Определяем переменные, которые будут использоваться в запросе
            String clientIdValue = document.getClientId();
            String contractUUID = UUID.randomUUID().toString();
            String signUUID = UUID.randomUUID().toString();
            String currentTime = getCurrentTimestamp();
            //String expireTime = getExpireTimestamp(currentTime);
            // ========= Заголовок ClientMessage и метаданные запроса =========
            org.w3c.dom.Document root = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element rootElement = root.createElementNS("urn://x-artefacts-smev-gov-ru/services/service-adapter/types", "tns:ClientMessage");
            root.appendChild(rootElement);
            Element itSystem = root.createElement("tns:itSystem");
            itSystem.appendChild(root.createTextNode(document.getMnemonic()));
            rootElement.appendChild(itSystem);
            Element requestMessage = root.createElement("tns:RequestMessage");
            rootElement.appendChild(requestMessage);
            Element requestMetadata = root.createElement("tns:RequestMetadata");
            requestMessage.appendChild(requestMetadata);
            Element clientId = root.createElement("tns:clientId");
            clientId.appendChild(root.createTextNode(clientIdValue));
            requestMetadata.appendChild(clientId);
            Element requestContent = root.createElement("tns:RequestContent");
            requestMessage.appendChild(requestContent);
            Element content = root.createElement("tns:content");
            requestContent.appendChild(content);
            Element messagePrimaryContent = root.createElement("tns:MessagePrimaryContent");
            content.appendChild(messagePrimaryContent);
            // ======= Бизнес-запрос вида сведений =======
            Element requestSigContract = root.createElementNS("urn://gosuslugi/sig-contract-ukep/1.0.0", "sig:RequestSignUkep");
            requestSigContract.setAttribute("Id", "ID_" + clientIdValue);
            requestSigContract.setAttribute("timestamp", currentTime);
            requestSigContract.setAttribute("routeNumber", "MNSV03");
            messagePrimaryContent.appendChild(requestSigContract);
            Element OID = root.createElement("sig:OID");
            OID.appendChild(root.createTextNode(document.getOid()));
            requestSigContract.appendChild(OID);
            Element signExp = root.createElement("sig:signExp");
            //signExp.appendChild(root.createTextNode(expireTime));
            signExp.appendChild(root.createTextNode(document.getSignExp()));
            requestSigContract.appendChild(signExp);
            Element descDoc = root.createElement("sig:descDoc");
            descDoc.appendChild(root.createTextNode(document.getDescDoc()));
            requestSigContract.appendChild(descDoc);
            Element contracts = root.createElement("sig:Contracts");
            requestSigContract.appendChild(contracts);
            Element contract = root.createElement("sig:Contract");
            contracts.appendChild(contract);
            Element docElement = root.createElement("sig:Document");
            docElement.setAttribute("docId", document.getDocumentId());
            docElement.setAttribute("mimeType", "application/pdf");
            docElement.setAttribute("uuid", contractUUID);
            docElement.setAttribute("description", document.getDocumentDescription());
            contract.appendChild(docElement);
            Element signElement = root.createElement("sig:Signature");
            signElement.setAttribute("docId", "Подпись оператора");
            signElement.setAttribute("mimeType", "application/sig");
            signElement.setAttribute("uuid", signUUID);
            contract.appendChild(signElement);
            Element backlink = root.createElement("sig:Backlink");
            backlink.appendChild(root.createTextNode(backLinkURL));
            requestSigContract.appendChild(backlink);
            // ========== Блок вложений ClientMessage =========
            Element attachmentHeaderList = root.createElement("tns:AttachmentHeaderList");
            content.appendChild(attachmentHeaderList);
            Element docAttachmentHeader = root.createElement("tns:AttachmentHeader");
            attachmentHeaderList.appendChild(docAttachmentHeader);
            Element contractId = root.createElement("tns:Id");
            contractId.appendChild(root.createTextNode(contractUUID));
            docAttachmentHeader.appendChild(contractId);
            Element docPath = root.createElement("tns:filePath");
            docPath.appendChild(root.createTextNode(document.getDocumentPath()));
            docAttachmentHeader.appendChild(docPath);
            Element docTransferMethod = root.createElement("tns:TransferMethod");
            docTransferMethod.appendChild(root.createTextNode("REFERENCE"));
            docAttachmentHeader.appendChild(docTransferMethod);
            Element signAttachmentHeader = root.createElement("tns:AttachmentHeader");
            attachmentHeaderList.appendChild(signAttachmentHeader);
            Element signId = root.createElement("tns:Id");
            signId.appendChild(root.createTextNode(signUUID));
            signAttachmentHeader.appendChild(signId);
            Element signPath = root.createElement("tns:filePath");
            signPath.appendChild(root.createTextNode(document.getDocumentPath() + ".sig"));
            signAttachmentHeader.appendChild(signPath);
            Element signTransferMethod = root.createElement("tns:TransferMethod");
            signTransferMethod.appendChild(root.createTextNode("REFERENCE"));
            signAttachmentHeader.appendChild(signTransferMethod);
            return getStringFromDocument(root);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new XMLTransformationException("Не удалось сформировать XML с запросом");
        }
    }

    private String getStringFromDocument(org.w3c.dom.Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return dateFormat.format(new Date());
    }

    private String getExpireTimestamp(String currentTimestamp) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Date current = dateFormat.parse(currentTimestamp);
        Calendar expired = Calendar.getInstance();
        expired.setTime(current);
        expired.add(Calendar.DATE, 1); // У абонента есть сутки на подписание документа
        return dateFormat.format(expired.getTime());
    }

    private String getFileContentEncoded(File file) throws FileOperationsException {
        try {
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = encoder.wrap(new ByteArrayOutputStream());
            int _byte;
            while ((_byte = fis.read()) != -1) {
                os.write(_byte);
            }
            os.close();
            return "";
        } catch (IOException e) {
            throw new FileOperationsException("Не удалось получить закодированное содержимое файла.");
        }
    }
}
