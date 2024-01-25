package ru.dob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {
    private final int requestLimit;
    private final long requestIntervalMillis;
    private final AtomicInteger requestCounter;
    private final Lock lock;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.requestIntervalMillis = timeUnit.toMillis(1);
        this.requestCounter = new AtomicInteger(0);
        this.lock = new ReentrantLock();
    }
    public static void main(String[] args) throws MalformedURLException {
        // создание объекта класса и указываем промежуток времени и количество запросов в этот промежуток
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 2);
        // создание списка products для класса document
        List<Product> products = new ArrayList<>();
        products.add(0,Product.builder().build());
        // создание url
        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
        // вызываем метод 4 раза для того, что бы проверить блокировку
        api.createDocument(Document.builder()
                .description(Description.builder().build())
                .products(products).build(),
                "123", url);
        api.createDocument(Document.builder()
                        .description(Description.builder().build())
                        .products(products).build(),
                "123", url);
        api.createDocument(Document.builder()
                        .description(Description.builder().build())
                        .products(products).build(),
                "123", url);
        api.createDocument(Document.builder()
                        .description(Description.builder().build())
                        .products(products).build(),
                "123", url);
    }
    public void createDocument(Document document,String signature,URL url) {
        lock.lock();
        try {
            // при превышении лимита запросов блокируется вызов
            int currentRequestCount = requestCounter.incrementAndGet();
            if (currentRequestCount > requestLimit) {
                Thread.sleep(requestIntervalMillis);
            }
            // документ конвертируется в Json объект
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String jsonDocument = writer.writeValueAsString(document);
            // вызывается по https метод post
            HttpsURLConnection con = getHttpsURLConnection(signature, jsonDocument, url);
            // вывод ответа
            System.out.println("Response code: " + con.getResponseCode());
            System.out.println("Response message: " + con.getResponseMessage());

        } catch (IOException | InterruptedException e) {
            e.getMessage();
        } finally {
            lock.unlock();
        }
    }
    // вызывается по https метод post
    private static HttpsURLConnection getHttpsURLConnection(String signature, String jsonDocument,URL url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type","application/json");
        con.setRequestProperty("product_document", Base64.getEncoder().encodeToString(jsonDocument.getBytes()));
        con.setRequestProperty("document_format", "MANUAL");
        con.setRequestProperty("type", "LP_INTRODUCE_GOODS");
        con.setRequestProperty("signature", signature);
        con.setDoOutput(true);
        con.setDoInput(true);
        return con;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        public Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean import_request;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String production_type;
        private List<Product> products;
        private LocalDate reg_date;
        private String reg_number;
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }
}
