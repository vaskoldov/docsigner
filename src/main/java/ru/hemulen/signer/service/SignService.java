package ru.hemulen.signer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.hemulen.crypto.exceptions.SignatureProcessingException;
import ru.hemulen.signer.exception.*;
import ru.hemulen.signer.model.Request;
import ru.hemulen.signer.signer.Signer;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

@Service
public class SignService {
    private static final Logger logger = LoggerFactory.getLogger(SignService.class);
    Signer signer;
    String containerAlias;
    String containerPassword;

    public SignService()  {
        // Загружаем алиас и пароль подписи из файла конфигурации
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("./config/config.ini"));
        } catch (IOException e) {
            System.err.println("Не удалось прочитать файл конфигурации.");
            e.printStackTrace(System.err);
            System.exit(1);
        }
        containerAlias = props.getProperty("CONTAINER_ALIAS");
        containerPassword = props.getProperty("CONTAINER_PASSWORD");
        try {
            signer = new Signer(containerAlias, containerPassword);

        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            logger.error("Ошибка при создании инстанса Signer");
            e.printStackTrace(System.err);
        }
    }

       public String processPKCS7(Request request) throws DocumentSignException, DocumentFileNotExists {
        File signFile = signPKCS7(request);
        return  signFile.toPath().toString();
    }

    public String processXMLDSig(Request request) throws DocumentSignException, DocumentFileNotExists, InvalidRequestParameters, InvalidXMLException {
        File signFile = signXMLDSig(request);
        return signFile.toPath().toString();
    }

    private File signPKCS7 (Request request) throws DocumentFileNotExists, DocumentSignException {
        File file = Paths.get(request.getFilePath()).toFile();
        if (!file.exists()) {
            throw new DocumentFileNotExists("Указанный файл c документом не найден");
        }
        try {
            File docSign = signer.signPKCS7Detached(file);
            return docSign;
        } catch (SignatureProcessingException | IOException e) {
            e.printStackTrace(System.err);
            throw new DocumentSignException("Не удалось подписать документ");
        }
    }
    private File signXMLDSig (Request request) throws DocumentFileNotExists, DocumentSignException, InvalidXMLException, InvalidRequestParameters {
        File file = Paths.get(request.getFilePath()).toFile();
        if (!file.exists()) {
            throw new DocumentFileNotExists("Указанный файл c документом не найден");
        }
        try {
            Element element = fileToElement(new File(request.getFilePath()));
            if (element == null) {
                throw new XMLTransformationException(String.format("Не удалось преобразовать файл %s в DOM объект", request.getFilePath()));
            }
            Element xmlSign = signer.signXMLDSigDetached(element, null);
            File signFile = new File(request.getFilePath() + ".sig");
            elementToFile(xmlSign, signFile);
            return signFile;
        } catch (SignatureProcessingException | IOException | ParserConfigurationException | SAXException | XMLTransformationException e) {
            e.printStackTrace(System.err);
            throw new DocumentSignException("Не удалось подписать документ");
        }
    }

    private Element fileToElement(File file) throws IOException, ParserConfigurationException, SAXException {
        if (file == null) {
            return null;
        }
        try (InputStream inputStream = new FileInputStream(file)) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setCoalescing(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            return documentBuilderFactory.newDocumentBuilder().parse(inputStream).getDocumentElement();
        }
    }

    private void elementToFile(Element element, File file) throws InvalidRequestParameters, XMLTransformationException {
        if (element == null || file == null) {
            throw new InvalidRequestParameters("Не указан файл для элемента с подписью XMLDSig или сам элемент с подписью");
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(file);
            Source input = new DOMSource(element);
            transformer.transform(input, output);
            return;
        } catch (TransformerException | IOException e) {
            throw new XMLTransformationException("Не удалось записать c подписью XMLDSign в файл");
        }
    }
}
