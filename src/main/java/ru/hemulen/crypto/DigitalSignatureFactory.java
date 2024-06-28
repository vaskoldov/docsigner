package ru.hemulen.crypto;

import org.apache.xml.security.Init;
import org.apache.xml.security.exceptions.AlgorithmAlreadyRegisteredException;
import org.apache.xml.security.transforms.Transform;
import ru.hemulen.crypto.exceptions.SigLibInitializationException;
import ru.hemulen.crypto.impl.CacheOptions;
import ru.hemulen.crypto.impl.CachingKeyStoreWrapper;
import ru.hemulen.crypto.impl.DigitalSignatureProcessorImpl;
import ru.hemulen.crypto.impl.SmevTransformSpi;
import ru.hemulen.crypto.impl.jcp.KeyStoreWrapperJCP;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class DigitalSignatureFactory {
    private static String providerName = "JCP2";
    private static volatile DigitalSignatureProcessor signatureProcessor = null;
    private static volatile KeyStoreWrapper keyStoreWrapper = null;

    public static synchronized void init(String keyStoreName) throws SigLibInitializationException {
        // Выполняем инициализацию фабрики
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        System.setProperty("org.apache.xml.security.resource.config", "resource/jcp.xml");
        initXmlSec();

        // Инициализируем процессор для подписи
        signatureProcessor = new DigitalSignatureProcessorImpl();
        try {
            keyStoreWrapper = new KeyStoreWrapperJCP(keyStoreName);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new SigLibInitializationException("Ошибка при инициализации signatureProcessor");
        }
    }

    private static void initXmlSec() throws SigLibInitializationException {
        try {
            Init.init();
            try {
                Transform.register(SmevTransformSpi.ALGORITHM_URN, SmevTransformSpi.class.getName());
            } catch (AlgorithmAlreadyRegisteredException e) {
                // Игнорируем это исключение
            }
        } catch (Exception e) {
            throw new SigLibInitializationException("Возникли проблемы при инициализации XmlSec!", e);
        }
    }

    public static DigitalSignatureProcessor getDigitalSignatureProcessor() throws SigLibInitializationException {
        DigitalSignatureProcessor p = signatureProcessor;
        if (p == null) {
            throw new SigLibInitializationException("Перед использованием фабрику необходимо инициализировать!");
        } else {
            return p;
        }
    }

    public static KeyStoreWrapper getKeyStoreWrapper() throws SigLibInitializationException {
        return getKeyStoreWrapper(null);
    }

    public static KeyStoreWrapper getKeyStoreWrapper(CacheOptions options) throws SigLibInitializationException {
        ru.hemulen.crypto.KeyStoreWrapper ks = keyStoreWrapper;
        if (ks == null) {
            throw new SigLibInitializationException("Перед использованием фабрику необходимо инициализировать!");
        } else {
            return (options == null ? ks : new CachingKeyStoreWrapper(ks, options));
        }
    }

}
