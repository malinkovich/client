import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

@MultipartConfig
public class File {

//    protected String host = "http://localhost:8081/file";
//    protected String host = "http://server:8080/file"; //true
    protected String ip = "84.201.135.19";
    protected String host = "http://"+ip+":8082/server/file";
    protected String doTransaction(HttpServletRequest request) {
        System.out.println("start transaction file");
        Date d1 = new Date();
        String reply = send(request, host);
        Date d2 = new Date();
        System.out.println((d2.getTime() - d1.getTime()) + " reply: \n" + reply);
        if (!reply.isEmpty()) {
            return reply;
        }
        return null;
    }

    private String send (HttpServletRequest request, String host) {
        System.out.println("send file request: \n" + request);
        StringBuilder sbAnswer = new StringBuilder();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(host);

//            httpPost.setHeader("Content-type", "multipart/form-data"); //не нужно при MultipartEntityBuilder (автоматом добавит)
            // Создание MultipartEntity для отправки файлов
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            // Добавление файлов в MultipartEntity
            // Получение частей (parts) из запроса
            Collection<Part> parts = request.getParts();
            // Итерация по каждой части и добавление в MultipartEntityBuilder
            for (Part part : parts) {
                String submittedFileName = part.getSubmittedFileName();
                if (submittedFileName != null && !submittedFileName.trim().isEmpty()) { //обязательно, иначе будет ошибка удаления файлов томката
                    // Получение имени файла
                    String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
                    if (fileName != null && !fileName.isEmpty()) {
                        // Создание InputStreamBody из части
                        InputStreamBody inputStreamBody = new InputStreamBody(part.getInputStream(), ContentType.DEFAULT_BINARY, fileName);
                        // Добавление InputStreamBody в builder
                        builder.addPart(part.getName(), inputStreamBody);
                    }
                    //part.delete();
                }
            }

            // Построение и выполнение запроса
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() != 200) { //500
                System.out.println("Ошибка сервера: \n" + response.getStatusLine());
                return sbAnswer.toString();
            }

            HttpEntity responceEntity = response.getEntity();
            //прочитаем ответ от сервера
            if (responceEntity == null) {
                return sbAnswer.toString();
            }
            String lineSep = System.getProperty("line.separator");
            BufferedInputStream in = new BufferedInputStream(responceEntity.getContent());
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String readLine;
            while((readLine = br.readLine()) != null) {
                sbAnswer.append(readLine).append(lineSep);
            }
            br.close();

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }

        return sbAnswer.toString();
    }

    /**
     * конвертация файлов в массив байт - затруднительно для больших файлов
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * удаление временного файла томката .tmp по путю - не использовать
     * @param path
     */
    public static void deleteFile(String path) {
        //получение папки
        java.io.File folder = new java.io.File(path);
        java.io.File[] folders = folder.listFiles(); //метод listFiles() для получения массива объектов File для всех файлов и папок в папке.
        // Сортируем директории по дате последнего изменения в обратном порядке
        Arrays.sort(folders, Comparator.comparingLong(java.io.File::lastModified).reversed());

        String directoryPath = folders[0].getAbsolutePath() + "\\work\\Catalina\\localhost\\ROOT";

        // Путь к папке, содержащей временные файлы
//        String directoryPath = "C:\\Users\\РђР»РёРЅР°\\AppData\\Local\\JetBrains\\IntelliJIdea2023.3\\tomcat\\e5bdcd0c-cd95-4ef8-8cb5-e453a5f005d7\\work\\Catalina\\localhost\\ROOT";

        // Создаем объект File для представления папки
        java.io.File directory = new java.io.File(directoryPath);
        System.out.println("\t\t\ttest directory --> " + directory.getAbsolutePath());

        // Убедитесь, что путь существует и является директорией
        if (directory.exists() && directory.isDirectory()) {
            // Получаем все файлы в папке
            java.io.File[] files = directory.listFiles();

            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isFile() && !file.getName().contains("SESSION")) {
                        // Удаляем каждый файл
                        boolean success = file.delete();
                        if (success) {
                            System.out.println("Файл удален: " + file.getName());
                        } else {
                            System.out.println("Не удалось удалить файл: " + file.getName());
                        }
                    }
                }
            }
        } else {
            System.out.println("Директория не существует или не является папкой");
        }
    }
}
