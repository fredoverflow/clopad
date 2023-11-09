import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class Advent<Code> {
    public static final String CACHE_FOLDER = System.getProperty("user.home") + "/Downloads";
    public static final String CACHE_FORMAT = "%d_%02d.txt";
    public static final String REMOTE_FORMAT = "https://adventofcode.com/%d/day/%d/input";
    public static final String SESSION_COOKIE = "advent.txt";
    public static final ZoneId RELEASE_ZONE = ZoneId.of("US/Eastern");

    public static String get(int year, int day) throws IOException, InterruptedException {
        valiDate(year, day);
        String content;

        var cache = Paths.get(CACHE_FOLDER, String.format(CACHE_FORMAT, year, day));
        try {
            content = Files.readString(cache);
        } catch (NoSuchFileException notCachedYet) {
            content = downloadContent(year, day);
            Files.writeString(cache, content);
        }
        return content;
    }

    private static String downloadContent(int year, int day) throws IOException, InterruptedException {
        var client = HttpClient.newBuilder().build();

        var session = Files.readString(Paths.get(CACHE_FOLDER, SESSION_COOKIE)).trim();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(REMOTE_FORMAT, year, day)))
                .header("Cookie", "session=" + session)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return response.body();
        } else {
            throw new IOException("HTTP status code " + response.statusCode() + ", session cookie length " + session.length());
        }
    }

    private static void valiDate(int year, int day) {
        if (!between(1, day, 25)) throw new IllegalArgumentException("illegal day " + day);
        var now = ZonedDateTime.now(RELEASE_ZONE);
        if (!between(2015, year, now.getYear())) throw new IllegalArgumentException("illegal year " + year);

        var desired = ZonedDateTime.of(LocalDate.of(year, Month.DECEMBER, day), LocalTime.MIDNIGHT, RELEASE_ZONE);
        var seconds = ChronoUnit.SECONDS.between(now, desired);
        if (seconds > 0) {
            var hours = ChronoUnit.HOURS.between(now, desired);
            if (hours > 0) {
                throw new IllegalArgumentException(hours + " hours until release...");
            }
            throw new IllegalArgumentException(seconds + " seconds until release...");
        }
    }

    private static boolean between(int min, int value, int max) {
        return min <= value && value <= max;
    }
}
