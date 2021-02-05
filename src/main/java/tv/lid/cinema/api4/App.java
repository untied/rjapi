package tv.lid.cinema.api4;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.MediaType;

import java.io.File;
import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tv.lid.cinema.api4.config.Config;
import tv.lid.cinema.api4.controllers.MovieController;
import tv.lid.cinema.api4.controllers.ScheduleController;
import tv.lid.cinema.api4.models.CommonModel;
import tv.lid.cinema.api4.models.MovieModel;
import tv.lid.cinema.api4.models.ScheduleModel;
import tv.lid.cinema.api4.storages.DatabaseStorage;

// главный класс приложения
public class App extends Jooby {
    // различные варианты запуска приложения
    private static final String CMD_OPERATE   = "operate",
                                CMD_INSTALL   = "install",
                                CMD_UNINSTALL = "uninstall";

    // база данных
    private static DatabaseStorage dbs = null;

    // экземпляр приложения
    private static Jooby instance = null;

    // инициализация класса
    {
        decoder(MediaType.json, (ctx, type) -> {
            try {
                return (new ObjectMapper())
                    .readValue(
                        ctx.body().bytes(),
                        Class.forName(type.getTypeName())
                    );
            } catch (ClassNotFoundException | JsonProcessingException exc) {
                return null;
            }
        });

        encoder(MediaType.json, (ctx, result) -> {
            ctx.setDefaultResponseType(MediaType.json);

            try {
                return (new ObjectMapper()).writeValueAsBytes(result);
            } catch (JsonProcessingException exc) {
                return null;
            }
        });

        path("/api4", () -> {
            // фильмы
            final MovieController movCtr = new MovieController();

            get("/movies",        movCtr.list);
            get("/movies/{page}", movCtr.list);
            post("/movie",        movCtr.create);
            get("/movie/{id}",    movCtr.find);
            put("/movie",         movCtr.modify);
            delete("/movie/{id}", movCtr.kill);

            // сеансы
            final ScheduleController schCtr = new ScheduleController();

            get("/schedules/{movieId}",        schCtr.list);
            get("/schedules/{movieId}/{page}", schCtr.list);
            post("/schedule",                  schCtr.create);
            get("/schedule/{id}",              schCtr.find);
            put("/schedule",                   schCtr.modify);
            delete("/schedule/{id}",           schCtr.kill);
        });
    }

    // создание таблиц в базе данных
    private static void install() throws SQLException {
        MovieModel.createTable();
        ScheduleModel.createTable();
    }

    // удаление таблиц из базы данных
    private static void uninstall() throws SQLException {
        ScheduleModel.dropTable();
        MovieModel.dropTable();
    }

    // нормальная работа приложения
    private static void operate(final String[] args) {
        App.instance = createApp(args, ExecutionMode.DEFAULT, App::new);
        App.instance.start();
    }

    public static void main(final String[] args) {
        // файл конфигурации приложения
        final File file = args.length == 2 ? new File(args[1]) : null;

        // читаем конфигурацию приложения
        final Config cfg = file != null && file.exists() && file.isFile() && file.canRead()
            ? Config.load(file)
            : Config.load();

        if (cfg == null) {
            System.out.println("Unable to interpret the configuration file! Exiting...\n\n");
            return;
        }

        // инициализация соединения с БД и подключение
        try {
            // подключение к серверу БД
            App.dbs = DatabaseStorage.initialize(cfg.database);
            App.dbs.connect();

            // инициализация моделей
            CommonModel.initialize(App.dbs.dslContext());
        } catch (SQLException exc) {
            System.out.println("Unable to initialize the database storage! Exiting...\n\n");
            return;
        }

        // хук завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // разрыв соединения с БД
                try {
                    dbs.disconnect();
                } catch (SQLException exc) {
                    System.out.println("Unable to finalize the database storage!\n\n");
                }
            }
        });

        // разбор командной строки
        try {
            if (args.length == 0 || args[0].equals(App.CMD_OPERATE)) { // обычный режим
                App.operate(args);
            } else if (args[0].equals(App.CMD_INSTALL)) { // создание таблиц
                App.install();
            } else if (args[0].equals(App.CMD_UNINSTALL)) { // удаление таблиц
                App.uninstall();
            } else {
                throw new Exception();
            }
        } catch (SQLException exc) {
            System.out.println("SQL exception occured during the execution! Exiting...\n\n");
        } catch (Exception exc) {
            System.out.println("Incorrect command line arguments were specified! Exiting...\n\n");
        }
    }

    // проверка нормального режима работы приложения
    public static boolean listens() {
        return App.dbs != null && App.instance != null;
    }

    // принудительный останов приложения
    public static void halt() {
        if (App.dbs != null) {
            try {
                App.dbs.disconnect();
            } catch (Exception exc) {}
        }
        if (App.instance != null) {
            App.instance.stop();
        }
    }
}
