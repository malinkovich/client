import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

@MultipartConfig //чтобы использовать getPart/s (и getParameter ?) при форме multipart/form-data
public class Web extends HttpServlet {

//    protected String host = "http://server:8080/server"; //true
protected String ip = "84.201.135.19";
    protected String host = "http://"+ip+":8082/server/server";

    protected String result = null; //результат работы НС

    protected HashMap<String, String> radios = new HashMap<String, String>();
    {
        radios.put("class","Имя класса");
        radios.put("property","Имя свойства");
        radios.put("value","Значени свойства");
        radios.put("iri","IRI-экземпляра");
    }

    protected boolean checkUploadFile = false; //проверка, что загрузки файлов еще не было
    protected String objectOntology = null; //radio
    protected String property = null; //radio

    protected String style =
            "        .container {\n" +
                    "            border: 1px solid rgb(255, 255, 255);\n" +
                    "            display: flex;\n" +
                    "            /*align-items: center; !*дивы в середине экрана (по высоте)*!*/\n" +
                    "        }\n" +
                    "        .block {\n" +
                    "            width: calc(25vw); /* ширина экрана 20% от ширины vw */\n" +
                    "            height: calc(100vh); /* высота экрана - vh */\n" +
                    "            margin: 10px; /* отступ между блоками */\n" +
                    "            padding: 10px;\n" +
                    "            background-color: #f2f2f2; /* цвет фона блока */\n" +
                    "        }\n";

    protected Map<String, List<String>> elements = new HashMap<>();


    /**
     * true - только отмеченные radio не сохраняются
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8"); //устанавливаете кодировку символов в ответе
        PrintWriter out = resp.getWriter();
        out.println(
                "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "    <head>\n" +
                        "        <meta http-equiv=\"Content-Type\" content=\"text/html\" charset=\"utf-8\">\n" +
                        "        <title>Главная</title>\n" +
                        "\n" +
                        "        <style>\n" + style +
                        "        </style>\n" +
                        "    </head>\n" +
                        "\n" +
                        "    <body>"
        );

        out.println("<div class=\"container\">");

        if (!checkUploadFile) {
            //форма загрузки файлов: НС, онтологии и фото
            out.println("<div class=\"block\">");
            out.println("<form method=\"post\"\n" +
                    "      enctype=\"multipart/form-data\">\n" +
                    "    <p>Выберите файлы:</p>" +
                    "    <ul>Допускаются следующие форматы:" +
                    "    <li>Онтология .rdf</li>" +
                    "    <li>Модель сверточной нейронной сети .pt</li>" +
                    "    <li>Изображение для распознавания .jpg или .png</li>" +
                    "    </ul>" +
                    "    <input type=\"file\" name=\"file\" size=\"100\" multiple/>\n" +
                    "    <br/>\n" +
                    "    <input name=\"upload\" type=\"submit\" value=\"Загрузить\"/>\n" +
                    "</form>");
            out.println("</div>");
        }

        //0 и 1 - загрузка файлов на сервер
        if (req.getParameter("upload") != null) {
            String response = new File().doTransaction(req); //запрос для передачи файлов
            if (!response.isEmpty()) {
                //конвертирую полученный результат в json - ключ=значение --> формат, имя файла
                Map<String, JsonElement> result = new Gson().fromJson(response, JsonObject.class).asMap();
                if (!result.containsKey("error")) {
                    //отображение загруженных файлов - названий
                    out.println("<div class=\"block\">");
                    out.println("<h3>Загруженные файлы:</h3><br><p>");
                    for (String format : result.keySet()) {
                        out.println(result.get(format).getAsString() + "<br>");
                    }
                    out.println("</p>");

                    //форма запуска НС (для распознавания)
                    out.println("<form method=\"post\">\n" +
                            "    <input name=\"startNetwork\" type=\"submit\" value=\"Запустить распознавание\"/>\n" +
                            "</form>");
                    out.println("</div>");
                    checkUploadFile = true;
                } else {
                    out.println("Неподходящий формат файлов: "); //проверка на форматы файлов
                    int i = 0;
                    for (String format : result.keySet()) { //т.к. первый параметр error: true
                        if (i++ == 0) continue; // Пропускаем первый элемент
                        out.println(result.get(format).getAsString() + "<br>");
                    }
                    out.println("Повторите загрузку файлов!");
                }
            } else out.println("Ошибка при загрузке файлов! Повторите загрузку.");
        }

        //2 - запуск модели нейронной сети
        if (req.getParameter("startNetwork") != null) { //запуск распознавания
            JsonObject request = new JsonObject();
            request.addProperty("startNetwork", "true");
            String response = doTransaction(String.valueOf(request), host);
            if (response != null) {

                //конвертирую полученный результат в json - ключ=значение --> формат, имя файла
                Map<String, JsonElement> result = new Gson().fromJson(response, JsonObject.class).asMap();
                if (!result.containsKey("error")) {
                    this.result = result.get("networkResult").getAsString();
                } else {
                    out.println("<h2>Ошибка при распознавании! Повторите загрузку или попробуйте использовать другую модель нейронной сети.</h2>");
                }
            }
        }

        //3 - выбор чем является результат нейронной сети в онтологии
        if (this.result != null) {
            //выбрана кнопка на форме
            if (req.getParameter("findObjectOntology") != null) {
                JsonObject request = new JsonObject();
                this.objectOntology = req.getParameter("objectOntology"); //ckecked
                request.addProperty("findObjectOntology", "true");
                request.addProperty("objectOntology", this.objectOntology);
                String response = doTransaction(String.valueOf(request), host);

                if (response != null) {
                    //конвертирую полученный результат в json - ключ=значение --> map<поле, коллекция элементов>
//                    Map<String, List<String>> elements = convertJsonArrayToList(response);
                    this.elements = convertJsonArrayToList(response);
                }
            }

            //форма
            out.println("<h3>Результат распознавания<br>нейронной сети: " + this.result + "</h3>");

            out.println("<div class=\"block\">");
            out.println("<form method=\"POST\">\n");
            out.println("<p>Выберите, чем является результат распознавания в онтологии:</p>");

            int i = 0; //счетчик элементов, чтобы отметить первый элемент
            for (String key : radios.keySet()) {
                boolean checkRadio =
                        this.objectOntology != null && this.objectOntology.equals(key); //значение radio было выбрано
                out.println("<p><input name=\"objectOntology\" type=\"radio\" value=\"" + key + "\"" +
//                        (i++ == 0 ? "checked" : "") + ">" +
                        (checkRadio ? "checked" : (i==0 ? "checked" : "")) + ">" + //либо уже выбранное значение, либо первое по дефолту
                        radios.get(key) + "</p>");
                i++;
            }
            out.println("<p><input type=\"submit\" name=\"findObjectOntology\" value=\"Найти в онтологии\"></p>");
            out.println("</form>");
            out.println("</div>");
        }

        //5 - форма для выбора экземпляра (на основе свойства)
        /*if (req.getParameter("findIndividualsOntology") != null) {
            //String property = req.getParameter("property"); //checked
            this.property = req.getParameter("property"); //checked

            JsonObject request = new JsonObject();
            request.addProperty("findIndividualsOntology", "true");
            request.addProperty("property", this.property);
            String response = doTransaction(String.valueOf(request), host);

            if (response != null) {
                Map<String, List<String>> elements = convertJsonArrayToList(response);
                if (!elements.containsKey("error")) {
                    List<String> individuals = elements.get("individuals");
                    out.println("<div class=\"block\">");
                    out.println("<form method=\"POST\">\n");
                    out.println("<p>Выберите интересующий экземпляр:</p>");
                    int i = 0;
                    for (String individual : individuals) {
                        out.println("<p><input name=\"individual\" type=\"radio\" value=\"" + individual + "\"" +
                                (i==0 ? "checked" : "") + ">" +
                                individual + "</p>");
                        i++;
                    }
                    out.println("<p><input type=\"submit\" name=\"findPropertyIndividual\" value=\"Получить характеристики\"></p>");
                    out.println("</form>");
                    out.println("</div>");
                } else {
                    out.println("<h2>Индивиды не были найдены! Повторите загрузку файла или попробуйте использовать другую онтологию.</h2>");
                }
            }
        }*/

        //5 - форма для выбора экземпляра (на основе свойства)
        StringBuilder state5 = new StringBuilder();
        if (req.getParameter("findIndividualsOntology") != null) {
            //String property = req.getParameter("property"); //checked
            this.property = req.getParameter("property"); //checked

            JsonObject request = new JsonObject();
            request.addProperty("findIndividualsOntology", "true");
            request.addProperty("property", this.property);
            String response = doTransaction(String.valueOf(request), host);

            if (response != null) {
                Map<String, List<String>> elements = convertJsonArrayToList(response);
                if (!elements.containsKey("error")) {
                    List<String> individuals = elements.get("individuals");
                    state5.append("<div class=\"block\">");
                    state5.append("<form method=\"POST\">\n");
                    state5.append("<p>Выберите интересующий экземпляр:</p>");
                    int i = 0;
                    for (String individual : individuals) {
                        state5.append("<p><input name=\"individual\" type=\"radio\" value=\"" + individual + "\"" +
                                (i==0 ? "checked" : "") + ">" +
                                individual + "</p>");
                        i++;
                    }
                    state5.append("<p><input type=\"submit\" name=\"findPropertyIndividual\" value=\"Получить характеристики\"></p>");
                    state5.append("</form>");
                    state5.append("</div>");
                } else {
                    state5.append("<h2>Индивиды не были найдены! Повторите загрузку файла или попробуйте использовать другую онтологию.</h2>");
                }
            }
        }

        //4 - форма для выбора свойства
        if (!this.elements.containsKey("error")) {
            List<String> objects = this.elements.get("elements");
            if (this.objectOntology.equals("value")) {
                out.println("<div class=\"block\">");
                out.println("<form method=\"POST\">\n");
                out.println("<p>Выберите, значением какого литерального свойства является результат распознавания в онтологии:</p>");
                int i = 0;
                for (String property : objects) {
                    boolean checkRadio =
                            this.property != null && this.property.equals(property); //значение radio было выбрано
                    out.println("<p><input name=\"property\" type=\"radio\" value=\"" + property + "\"" +
//                            (i==0 ? "checked" : "") + ">" +
                            (checkRadio ? "checked" : (i==0 ? "checked" : "")) + ">" +
                            property + "</p>");
                    i++;
                }
                out.println("<p><input type=\"submit\" name=\"findIndividualsOntology\" value=\"Найти экземпляры в онтологии\"></p>");
                out.println("</form>");
                out.println("</div>");
            } else {
                out.println("<div class=\"block\">");
                for (String object : objects) {
                    out.println("<p>" + object + "</p>");
                }
                out.println("</div>");
            }
        } else {
            out.println("<h2>Свойства не были найдены! Повторите загрузку файла или попробуйте использовать другую онтологию.</h2>");
        }
        out.println(state5); //для корректно отображения выбранных radio

        //6 - получение характеристик (свойство-значение) индивида (на основе индивида)
        if (req.getParameter("findPropertyIndividual") != null) {
            String individual = req.getParameter("individual"); //checked

            JsonObject request = new JsonObject();
            request.addProperty("findPropertyIndividual", "true");
            request.addProperty("individual", individual);

            String response = doTransaction(String.valueOf(request), host);

            if (response != null) {
//                Map<String, List<String>> elements = convertJsonArrayToList(response);
//                if (!elements.containsKey("error")) {
//                    List<String> properties = elements.get("properties");
//                    List<String> values = elements.get("individuals");
//
//                    out.println("<div class=\"block\">");
//                    out.println("<p>Выбранный индивид: " + individual + "</p>");
//                    for (int i = 0; i < properties.size(); i++) {
//                        out.println("<p>" + properties.get(i) + " - " +
//                                values.get(i) + "</p>");
//                    }
//                    out.println("</div>");
//                } else {
//                    out.println("<h2>Информация об индивиде не была найдена! Повторите загрузку файла или попробуйте использовать другую онтологию.</h2>");
//                }

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> properties = gson.fromJson(jsonObject.get("properties"), listType);
                List<String> values = gson.fromJson(jsonObject.get("values"), listType);

                if (properties != null && !properties.isEmpty() &&
                        values != null && !values.isEmpty()) {
                    out.println("<div class=\"block\">");
                    out.println("<p>Выбранный индивид: " + individual + "</p>");
                    for (int i = 0; i < properties.size(); i++) {
                        out.println("<p>" + properties.get(i) + " - " +
                                values.get(i) + "</p>");
                    }
                    out.println("</div>");
                } else {
                    out.println("<h2>Информация об индивиде не была найдена! Повторите загрузку файла или попробуйте использовать другую онтологию.</h2>");
                }
            }
        }

        out.println(
                "</div></body>" +
                        "</head>" +
                        "</html>");
        out.close();
    }

    protected Map<String, List<String>> convertJsonArrayToList(String response) {
        // Конвертируем строку в JsonObject
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

        // Создаем Map для хранения результатов
        Map<String, List<String>> resultMap = new HashMap<>();

        // Получаем набор ключей из JsonObject
        Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();

        // Итерируем по каждому элементу набора
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            // Проверяем, является ли значение JsonArray
            if (entry.getValue().isJsonArray()) {
                // Получаем JsonArray
                JsonArray jsonArray = entry.getValue().getAsJsonArray();
                // Создаем список строк
                List<String> stringList = new ArrayList<>();
                // Итерируем по JsonArray и добавляем элементы в список строк
                for (JsonElement element : jsonArray) {
                    stringList.add(element.getAsString());
                }
                // Добавляем список строк в resultMap
                resultMap.put(entry.getKey(), stringList);
            } else resultMap.put(entry.getKey(), null); //если ошибка
        }
        return resultMap;
    }

    /**
     *
     * @param request
     * @param host
     * @return
     */
    protected String doTransaction(String request, String host) {
        System.out.println("start transaction");
        Date d1 = new Date();
        String reply = send(request, host);
        Date d2 = new Date();
        System.out.println((d2.getTime() - d1.getTime()) + " reply: \n" + reply);
        if (!reply.isEmpty()) {
            return reply;
        }
        return null;
    }

    private String send(String request, String host) {
        System.out.println("send request: \n" + request);
        StringBuilder sbAnswer = new StringBuilder();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(host);
            httpPost.setHeader("Content-type", "application/json");
            HttpEntity entity = new ByteArrayEntity(request.getBytes("UTF-8"));
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sbAnswer.toString();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
