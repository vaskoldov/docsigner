package ru.hemulen.signer.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import ru.hemulen.crypto.exceptions.SignatureProcessingException;
import ru.hemulen.signer.signer.Signer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

//@Configuration
public class FileProcessor extends Thread {

    static private Logger log = LoggerFactory.getLogger(FileProcessor.class);
    private String inputDir;
    private String outputDir;
    private long sleepTime;
    private Signer signer;

    public FileProcessor() {

    }

    //@Bean
    public void run() {
        log.info("Запуск процесса опроса каталога с файлами на подпись");
        Properties props = new Properties();
        try {
            props.load(new FileReader("./config/config.ini"));
        } catch (IOException e) {
            log.error("Не удалось прочитать файл настроек");
            System.exit(1);
        }
        inputDir = props.getProperty("PATH_IN");
        outputDir = props.getProperty("PATH_OUT");
        sleepTime = Long.parseLong(props.getProperty("SLEEP_TIME"));
        try {
            signer = new Signer(props.getProperty("CONTAINER_ALIAS"), props.getProperty("CONTAINER_PASSWORD"));
        } catch (Exception e) {
            System.err.println("Не удалось инициализировать крипто-модуль");
            e.printStackTrace(System.err);
            System.exit(1);
        }

        Path pathIn = Paths.get(inputDir);
        Path pathOut = Paths.get(outputDir);
        try {
            if (!pathIn.toFile().exists()) Files.createDirectories(pathIn);
            if (!pathOut.toFile().exists()) Files.createDirectories(pathOut);
        } catch (IOException e) {
            log.error("Не удалось создать входной или выходной каталоги");
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        while (true) {
            File[] files = pathIn.toFile().listFiles();
            if (files.length == 0) {
                try {
                    sleep(sleepTime);
                } catch (InterruptedException e) {
                    log.error("Ошибка при выполнении команды sleep");
                    throw new RuntimeException(e);
                }
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }
                log.info("Подписание файла {}", file.getName());
                try {
                    File sigFile = signer.signPKCS7Detached(file);
                    Files.move(file.toPath(), pathOut.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
                    Files.move(sigFile.toPath(), pathOut.resolve(sigFile.getName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (SignatureProcessingException | IOException e) {
                    log.error("Ошибка при подписании файла {}", file.getName());
                    // При ошибке подписания оставляем файл в исходном каталоге для последующих попыток
                    continue;
                }
            }
        }
    }
}
