package tv.lid.cinema.api4.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.junit.jupiter.api.Assertions.*;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import tv.lid.cinema.api4.App;
import tv.lid.cinema.api4.models.CommonModel;
import tv.lid.cinema.api4.models.MovieModel;
import tv.lid.cinema.api4.models.ScheduleModel;

@TestMethodOrder(OrderAnnotation.class)
public final class AppTest {
    // JSON media type
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // клиент OkHttp
    private static final OkHttpClient client = new OkHttpClient();

    // префикс URL для эндпойнтов
    private static final String API_URL_PREFIX = "http://localhost:8080/api4";

    // путь к тестовому файлу конфигурации
    private static String cfgPath = null;

    // обертка списка записей
    @JsonInclude(Include.NON_NULL)
    private static final class ListWrapper<T extends CommonModel> {
        @JsonProperty(value = "list", required = true)
        public final List<T> list; // сам список

        @JsonProperty(value = "total", required = false, defaultValue = "0")
        public final int total; // всего записей

        @JsonProperty(value = "pages", required = false, defaultValue = "0")
        public final int pages; // количество страниц

        // конструктор #1
        @JsonCreator
        public ListWrapper(
            @JsonProperty("list")  final List<T> list,
            @JsonProperty("total") final int     total,
            @JsonProperty("pages") final int     pages
        ) {
            this.list  = list;
            this.total = total;
            this.pages = pages;
        }

        // конструктор #2
        public ListWrapper() {
            this(new ArrayList<T>(), 0, 0);
        }
    }

    @JsonInclude(Include.NON_NULL)
    private static class Result {
        @JsonProperty(value = "code", required = true)
        public final int code;  // код ответа

        @JsonProperty(value = "info", required = false)
        public final String info; // дополнительная информация

        // конструктор
        @JsonCreator
        public Result(
            @JsonProperty("code") final int    code,
            @JsonProperty("info") final String info
        ) {
            this.code = code;
            this.info = info;
        }
    }

    // обёртка ответа сервера для одной записи
    @JsonInclude(Include.NON_NULL)
    private static class ResultOne<T extends CommonModel> extends AppTest.Result {
        @JsonProperty(value = "data", required = true)
        public final T data; // данные

        // конструктор #1
        @JsonCreator
        public ResultOne(
            @JsonProperty("code") final int    code,
            @JsonProperty("info") final String info,
            @JsonProperty("data") final T      data
        ) {
            super(code, info);
            this.data = data;
        }

        // конструктор #2
        public ResultOne() {
            this(0, "", null);
        }
    }

    // обёртка ответа сервера для списка записей
    @JsonInclude(Include.NON_NULL)
    private static class ResultList<T extends CommonModel> extends AppTest.Result {
        @JsonProperty(value = "data", required = true)
        public final AppTest.ListWrapper<T> data; // данные

        // конструктор #1
        @JsonCreator
        public ResultList(
            @JsonProperty("code") final int    code,
            @JsonProperty("info") final String info,
            @JsonProperty("data") final AppTest.ListWrapper<T> data
        ) {
            super(code, info);
            this.data = data;
        }

        // конструктор #2
        public ResultList() {
            this(0, "", null);
        }
    }

    @BeforeAll
    public static void startServer() {
        // тестовый файл конфигурации сервера
        final File cfgFile = Paths.get("src", "test", "resources", "config.json").toFile();
        if (cfgFile.exists() && cfgFile.isFile() && cfgFile.canRead()) {
            // сохраняем путь к файлу конфигурации
            AppTest.cfgPath = cfgFile.getAbsolutePath();

            // создаём таблицы в базе данных
            App.main(new String[] { "install", AppTest.cfgPath });
            App.halt();

            // запускаем сервер
            App.main(new String[] { "operate", AppTest.cfgPath });

            // даём время на запуск (10 секунд)
            try {
                Thread.sleep(10000);
            } catch (InterruptedException exc) {}
        }
    }

    @Test
    @Order(10)
    @DisplayName("Application was started")
    public void appWasStarted() {
        // проверяем, что приложение запущено
        assertTrue(
            App.listens(),
            "Failed to start the application!"
        );
    }

    @Test
    @Order(20)
    @DisplayName("Get the list of movies #1")
    public void getMoviesList1() {
        // запрашиваем список фильмов
        final AppTest.ListWrapper<MovieModel> data = AppTest.__getMovies();
        if (data != null) {
            assertTrue(
                data.list.size() == 0 && data.total == 0 && data.pages == 0,
                "Wrong data was received from server!"
            );
        }
    }

    @Test
    @Order(30)
    @DisplayName("Add the movie #1")
    public void addMovie1() {
        // добавляем фильм
        AppTest.__addMovie("Заключенный номер 13", (short) 20, (short) 1920);
    }

    @Test
    @Order(40)
    @DisplayName("Add the movie #2")
    public void addMovie2() {
        // добавляем фильм
        AppTest.__addMovie("Шерлок младший", (short) 45, (short) 1924);
    }

    @Test
    @Order(50)
    @DisplayName("Add the movie #3")
    public void addMovie3() {
        // добавляем фильм
        AppTest.__addMovie("Генерал", (short) 75, (short) 1926);
    }

    @Test
    @Order(60)
    @DisplayName("Get the list of movies #2")
    public void getMoviesList2() {
        // запрашиваем список фильмов
        final AppTest.ListWrapper<MovieModel> data = AppTest.__getMovies();
        if (data != null) {
            // проверяем количество фильмов
            if (data.list.size() == 3) {
                MovieModel movie;

                // проверяем список фильмов
                for (int i = 0; i < 3; i++) {
                    movie = data.list.get(i);
                    switch (movie.id) {
                        case 1:
                            assertTrue(
                                movie.title.equals("Заключенный номер 13") && movie.duration == 20 && movie.year == 1920,
                                "Wrong data of the movie #1 was discovered!"
                            );
                            break;
                        case 2:
                            assertTrue(
                                movie.title.equals("Шерлок младший") && movie.duration == 45 && movie.year == 1924,
                                "Wrong data of the movie #2 was discovered!"
                            );
                            break;
                        case 3:
                            assertTrue(
                                movie.title.equals("Генерал") && movie.duration == 75 && movie.year == 1926,
                                "Wrong data of the movie #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a movie!");
                    }
                }
            } else {
                fail("Wrong number of movies was received from server!");
            }
        }
    }

    @Test
    @Order(70)
    @DisplayName("Get the specified movie")
    public void getMovie() {
        // запрашиваем фильм по заданному идентификатору
        final MovieModel movie = AppTest.__getMovie(1);
        if (movie != null) {
            assertTrue(
                movie.title.equals("Заключенный номер 13") && movie.duration == 20 && movie.year == 1920,
                "Wrong data of the movie having id=1 was discovered!"
            );
        }
    }

    @Test
    @Order(80)
    @DisplayName("Modify the movie")
    public void modifyMovie() {
        // изменяем фильм с идентификатором 1
        AppTest.__modifyMovie(1, "Копы", (short) 18, (short) 1922);
    }

    @Test
    @Order(90)
    @DisplayName("Get the list of movies #3")
    public void getMoviesList3() {
        // запрашиваем список фильмов
        final AppTest.ListWrapper<MovieModel> data = AppTest.__getMovies();
        if (data != null) {
            // проверяем количество фильмов
            if (data.list.size() == 3) {
                MovieModel movie;

                // проверяем список фильмов
                for (int i = 0; i < 3; i++) {
                    movie = data.list.get(i);
                    switch (movie.id) {
                        case 1:
                            assertTrue(
                                movie.title.equals("Копы") && movie.duration == 18 && movie.year == 1922,
                                "Wrong data of the movie #1 was discovered!"
                            );
                            break;
                        case 2:
                            assertTrue(
                                movie.title.equals("Шерлок младший") && movie.duration == 45 && movie.year == 1924,
                                "Wrong data of the movie #2 was discovered!"
                            );
                            break;
                        case 3:
                            assertTrue(
                                movie.title.equals("Генерал") && movie.duration == 75 && movie.year == 1926,
                                "Wrong data of the movie #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a movie!");
                    }
                }
            } else {
                fail("Wrong number of movies was received from server!");
            }
        }
    }

    @Test
    @Order(100)
    @DisplayName("Remove the movie")
    public void killMovie() {
        // удаляем фильм с идентификатором 2
        AppTest.__killMovie(2);
    }

    @Test
    @Order(110)
    @DisplayName("Get the list of movies #4")
    public void getMoviesList4() {
        // запрашиваем список фильмов
        final AppTest.ListWrapper<MovieModel> data = AppTest.__getMovies();
        if (data != null) {
            // проверяем количество фильмов
            if (data.list.size() == 2) {
                MovieModel movie;

                // проверяем список фильмов
                for (int i = 0; i < 2; i++) {
                    movie = data.list.get(i);
                    switch (movie.id) {
                        case 1:
                            assertTrue(
                                movie.title.equals("Копы") && movie.duration == 18 && movie.year == 1922,
                                "Wrong data of the movie #1 was discovered!"
                            );
                            break;
                        case 2:
                            fail("The inappropriate movie record was discovered!");
                            break;
                        case 3:
                            assertTrue(
                                movie.title.equals("Генерал") && movie.duration == 75 && movie.year == 1926,
                                "Wrong data of the movie #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a movie!");
                    }
                }
            } else {
                fail("Wrong number of movies was received from server!");
            }
        }
    }

    @Test
    @Order(120)
    @DisplayName("Get the list of schedules #1")
    public void getSchedulesList1() {
        // запрашиваем список сеансов
        final AppTest.ListWrapper<ScheduleModel> data = AppTest.__getSchedules(3);
        if (data != null) {
            assertTrue(
                data.list.size() == 0 && data.total == 0 && data.pages == 0,
                "Wrong data was received from server!"
            );
        }
    }

    @Test
    @Order(130)
    @DisplayName("Add the schedule #1")
    public void addSchedule1() {
        // добавляем сеанс
        AppTest.__addSchedule(3, "2020-09-10 21:00", (byte) 2);
    }

    @Test
    @Order(140)
    @DisplayName("Add the schedule #2")
    public void addSchedule2() {
        // добавляем сеанс
        AppTest.__addSchedule(3, "2020-09-11 21:00", (byte) 2);
    }

    @Test
    @Order(150)
    @DisplayName("Add the schedule #3")
    public void addSchedule3() {
        // добавляем сеанс
        AppTest.__addSchedule(3, "2020-09-12 21:00", (byte) 2);
    }

    @Test
    @Order(160)
    @DisplayName("Get the list of schedules #2")
    public void getSchedulesList2() {
        // запрашиваем список сеансов
        final AppTest.ListWrapper<ScheduleModel> data = AppTest.__getSchedules(3);
        if (data != null) {
            // проверяем количество сеансов
            if (data.list.size() == 3) {
                ScheduleModel schedule;

                // проверяем список сеансов
                for (int i = 0; i < 3; i++) {
                    schedule = data.list.get(i);
                    switch (schedule.id) {
                        case 1:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-10 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #1 was discovered!"
                            );
                            break;
                        case 2:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-11 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #2 was discovered!"
                            );
                            break;
                        case 3:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-12 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a schedule!");
                    }
                }
            } else {
                fail("Wrong number of schedules was received from server!");
            }
        }
    }

    @Test
    @Order(170)
    @DisplayName("Get the specified schedule")
    public void getSchedule() {
        // запрашиваем сеанс по заданному идентификатору
        final ScheduleModel schedule = AppTest.__getSchedule(2);
        if (schedule != null) {
            assertTrue(
                schedule.dateAndTime.equals("2020-09-11 21:00") && schedule.auditorium == 2,
                "Wrong data of the schedule having id=2 was discovered!"
            );
        }
    }

    @Test
    @Order(180)
    @DisplayName("Modify the schedule")
    public void modifySchedule() {
        // изменяем фильм с идентификатором 2
        AppTest.__modifySchedule(2, 3, "2020-09-11 22:00", (byte) 1);
    }

    @Test
    @Order(190)
    @DisplayName("Get the list of schedules #3")
    public void getSchedulesList3() {
        // запрашиваем список сеансов
        final AppTest.ListWrapper<ScheduleModel> data = AppTest.__getSchedules(3);
        if (data != null) {
            // проверяем количество сеансов
            if (data.list.size() == 3) {
                ScheduleModel schedule;

                // проверяем список сеансов
                for (int i = 0; i < 3; i++) {
                    schedule = data.list.get(i);
                    switch (schedule.id) {
                        case 1:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-10 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #1 was discovered!"
                            );
                            break;
                        case 2:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-11 22:00") && schedule.auditorium == 1,
                                "Wrong data of the schedule #2 was discovered!"
                            );
                            break;
                        case 3:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-12 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a schedule!");
                    }
                }
            } else {
                fail("Wrong number of schedules was received from server!");
            }
        }
    }

    @Test
    @Order(200)
    @DisplayName("Remove the schedule")
    public void killSchedule() {
        // удаляем сеанс с идентификатором 1
        AppTest.__killSchedule(1);
    }

    @Test
    @Order(210)
    @DisplayName("Get the list of schedules #4")
    public void getSchedulesList4() {
        // запрашиваем список сеансов
        final AppTest.ListWrapper<ScheduleModel> data = AppTest.__getSchedules(3);
        if (data != null) {
            // проверяем количество сеансов
            if (data.list.size() == 3) {
                ScheduleModel schedule;

                // проверяем список сеансов
                for (int i = 0; i < 2; i++) {
                    schedule = data.list.get(i);
                    switch (schedule.id) {
                        case 1:
                            fail("The inappropriate schedule record was discovered!");
                            break;
                        case 2:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-11 22:00") && schedule.auditorium == 1,
                                "Wrong data of the schedule #2 was discovered!"
                            );
                            break;
                        case 3:
                            assertTrue(
                                schedule.dateAndTime.equals("2020-09-12 21:00") && schedule.auditorium == 2,
                                "Wrong data of the schedule #3 was discovered!"
                            );
                            break;
                        default:
                            fail("Unknown identifier was discovered for a schedule!");
                    }
                }
            } else {
                fail("Wrong number of schedules was received from server!");
            }
        }
    }

    @AfterAll
    public static void stopServer() {
        if (AppTest.cfgPath != null) {
            // останавливаем сервер
            App.halt();

            // даём время на останов (5 секунд)
            try {
                Thread.sleep(5000);
            } catch (InterruptedException exc) {}

            // удаляем таблицы из базы данных
            App.main(new String[] { "uninstall", AppTest.cfgPath });
            App.halt();
        }
    }

    // запрашиваем список фильмов
    private static AppTest.ListWrapper<MovieModel> __getMovies() {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/movies").build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return null;
        }

        // расшифровываем ответ
        AppTest.ResultList<MovieModel> result;
        try {
            result = (new ObjectMapper()).readValue(
                content,
                new TypeReference<AppTest.ResultList<MovieModel>>() {}
            );
            if (result == null || result.code != 200) {
                throw new Exception();
            }
            return result.data;
        } catch (Exception exc) {
            fail("Failed to decode server's response!");
            return null;
        }
    }

    // запрашиваем фильм с заданным идентификатором
    private static MovieModel __getMovie(final int id) {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/movie/" + id).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return null;
        }

        // расшифровываем ответ
        AppTest.ResultOne<MovieModel> result;
        try {
            result = (new ObjectMapper()).readValue(
                content,
                new TypeReference<AppTest.ResultOne<MovieModel>>() {}
            );
            if (result == null || result.code != 200) {
                throw new Exception();
            }
            return result.data;
        } catch (Exception exc) {
            fail("Failed to decode server's response!");
            return null;
        }
    }

    // добавляем фильм по заданным параметрам
    private static void __addMovie(
        final String title,    // название
        final short  duration, // длительность в минутах
        final short  year      // год создания
    ) {
        // данные по фильму
        final String json = "{" +
            "\"title\": \"" + title + "\"," +
            "\"duration\": " + duration + "," +
            "\"year\": " + year +
        "}";

        // создаем запрос
        final RequestBody body    = RequestBody.create(AppTest.JSON, json);
        final Request     request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/movie").post(body).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // изменяем фильм с заданным идентификатором
    private static void __modifyMovie(
        final int    id,       // идентификатор
        final String title,    // название
        final short  duration, // длительность в минутах
        final short  year      // год создания
    ) {
        // данные по фильму
        final String json = "{" +
            "\"id\": " + id + "," +
            "\"title\": \"" + title + "\"," +
            "\"duration\": " + duration + "," +
            "\"year\": " + year +
        "}";

        // создаем запрос
        final RequestBody body    = RequestBody.create(AppTest.JSON, json);
        final Request     request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/movie").put(body).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // удаляем фильм с заданным идентификатором
    private static void __killMovie(final int id) {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/movie/" + id).delete().build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // запрашиваем список сеансов по заданному фильму
    private static AppTest.ListWrapper<ScheduleModel> __getSchedules(final int movieId) {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/schedules/" + movieId).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return null;
        }

        // расшифровываем ответ
        AppTest.ResultList<ScheduleModel> result;
        try {
            result = (new ObjectMapper()).readValue(
                content,
                new TypeReference<AppTest.ResultList<ScheduleModel>>() {}
            );
            if (result == null || result.code != 200) {
                throw new Exception();
            }
            return result.data;
        } catch (Exception exc) {
            fail("Failed to decode server's response!");
            return null;
        }
    }

    // запрашиваем сеанс с заданным идентификатором
    private static ScheduleModel __getSchedule(final int id) {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/schedule/" + id).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return null;
        }

        // расшифровываем ответ
        AppTest.ResultOne<ScheduleModel> result;
        try {
            result = (new ObjectMapper()).readValue(
                content,
                new TypeReference<AppTest.ResultOne<ScheduleModel>>() {}
            );
            if (result == null || result.code != 200) {
                throw new Exception();
            }
            return result.data;
        } catch (Exception exc) {
            fail("Failed to decode server's response!");
            return null;
        }
    }

    // добавляем сеанс по заданному фильму
    private static void __addSchedule(
        final int    movieId,     // идентификатор фильма
        final String dateAndTime, // дата и время
        final byte   auditorium   // номер зала
    ) {
        // данные по сеансу
        final String json = "{" +
            "\"movieId\": " + movieId + "," +
            "\"dateAndTime\": \"" + dateAndTime + "\"," +
            "\"auditorium\": " + auditorium +
        "}";

        // создаем запрос
        final RequestBody body    = RequestBody.create(AppTest.JSON, json);
        final Request     request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/schedule").post(body).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // изменяем сеанс с заданным идентификатором
    private static void __modifySchedule(
        final int    id,          // идентификатор сеанса
        final int    movieId,     // идентификатор фильма
        final String dateAndTime, // дата и время
        final byte   auditorium   // номер зала
    ) {
        // данные по сеансу
        final String json = "{" +
            "\"id\": " + id + "," +
            "\"movieId\": " + movieId + "," +
            "\"dateAndTime\": \"" + dateAndTime + "\"," +
            "\"auditorium\": " + auditorium +
        "}";

        // создаем запрос
        final RequestBody body    = RequestBody.create(AppTest.JSON, json);
        final Request     request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/schedule").put(body).build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // удаляем сеанс с заданным идентификатором
    private static void __killSchedule(final int id) {
        // создаем запрос
        final Request request = new Request.Builder().url(AppTest.API_URL_PREFIX + "/schedule/" + id).delete().build();

        String content; // данные ответа

        // отправляем запрос
        try {
            content = (AppTest.client.newCall(request).execute()).body().string();
        } catch (IOException exc) {
            fail("Failed to send a request to server!");
            return;
        }

        // проверяем ответ
        assertTrue(AppTest.__isOK(content), "Unsuccessful request sending result!");
    }

    // декодируем ответ с кодом 200 от сервера
    private static boolean __isOK(final String data) {
        try {
            final AppTest.Result result = (new ObjectMapper()).readValue(data, AppTest.Result.class);
            if (result == null || result.code != 200) {
                throw new Exception();
            }
            return true;
        } catch (Exception exc) {
            return false;
        }
    }
}
