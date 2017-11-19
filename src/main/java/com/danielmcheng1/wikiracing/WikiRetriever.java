package com.danielmcheng1.wikiracing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All the logic to make requests to the MediaWiki API is here. If the API changes in the future or if we want to crawl
 * another source all our changes would be localized to this class.
 */
public class WikiRetriever {
    private static final String wikiNamespaces = "0|14|100";
    private static final String wikiUserAgent = "wikiracer/1.0 (dcheng21@uchicago.edu)";
    private static final Logger LOGGER = Logger.getLogger(WikiRetriever.class.getName());
    // Avoid creating several instances, should be singleton
    public static final OkHttpClient client = new OkHttpClient();

    // based on input list of titles, build the WikiAPI URL and request a response
    public static JsonObject getWikiResponse(String titles, Direction direction, JsonObject continueParams) {
        JsonObject response = getResponse(buildWikiURL(titles, direction, continueParams), wikiUserAgent);
        return response;
    }

    // missing and invalid pages are marked with negative page IDs
    // https://www.mediawiki.org/wiki/API:Query#Missing_and_invalid_titles
    public static JsonObject getMissingInvalidPages(JsonObject response) {
        if (response == null) {
            return null;
        }
        JsonObject pageResults = response.getAsJsonObject("query").getAsJsonObject("pages");
        JsonObject missingInvalid = new JsonObject();
        for (String page : pageResults.keySet()) {
            if (Integer.valueOf(page) < 0) {
                missingInvalid.add(page, pageResults.get(page));
            }
        }
        return missingInvalid;
    }

    /*
    WikiMedia API returns up to 500 results at a time
    Continue key allows user to pick back up at the last result
    Example of continue parameters to pass back into query:
        "continue": {
            "blcontinue": "0|23982399",
            "continue": "-||"
        }
    */
    public static JsonObject getContinue(JsonObject response) {
        if (!response.has("continue")) {
            return null;
        }
        JsonObject continueJson = response.getAsJsonObject("continue");
        LOGGER.log(Level.INFO, getThread() + "Continue: " + continueJson);
        return continueJson;
    }

    // URL builder to create parameters needed for Wiki API call
    protected static HttpUrl buildWikiURL(String titles, Direction direction, JsonObject continueParams) {
        String prefix, prop;
        if (Direction.FORWARDS.equals(direction)) {
            prefix = "pl";
            prop = "links";
        } else if (Direction.BACKWARDS.equals(direction)) {
            prefix = "lh";
            prop = "linkshere";
        } else throw new IllegalArgumentException("Invalid direction specified: " + direction);

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
        urlBuilder.scheme("https").host("en.wikipedia.org").addPathSegment("w").addPathSegment("api.php");
        urlBuilder.addQueryParameter("action", "query");
        urlBuilder.addQueryParameter("titles", titles);
        urlBuilder.addQueryParameter("prop", prop);
        urlBuilder.addQueryParameter(prefix + "limit", "max");
        urlBuilder.addQueryParameter(prefix + "namespace", wikiNamespaces);
        urlBuilder.addQueryParameter("format", "json");

        if (continueParams != null) {
            for (String key : continueParams.keySet()) {
                urlBuilder.addQueryParameter(key, continueParams.get(key).getAsString());
            }
        }
        LOGGER.log(Level.INFO, getThread() + "Constructed wikiURL: " + urlBuilder.build());
        return urlBuilder.build();
    }

    // general method for requesting response using OkHTTP library
    private static JsonObject getResponse(HttpUrl url, String userAgent) {
        Request request = new Request.Builder()
                .header("User-Agent", userAgent)
                .url(url)
                .build();
        try {
            long before = System.currentTimeMillis();
            Response response = client.newCall(request).execute();
            long after = System.currentTimeMillis();
            long delta = after - before;
            LOGGER.log(Level.INFO, getThread() + "Delta for getResponse: " + delta);

            String jsonStringResponse = response.body().string();
            LOGGER.log(Level.INFO, getThread() + "Response body: " + jsonStringResponse);
            return new Gson().fromJson(jsonStringResponse, JsonObject.class);
        } catch (Exception e) {
            // Crawler class will back-off then attempt to call this again
            LOGGER.log(Level.INFO, getThread() + "Exception in getResponse: " + e.getMessage());
            return null;
        }
    }

    // for debugging
    public static String getWikiNamespaces() {
        return wikiNamespaces;
    }

    public static void setLogLevel(Level level) {
        LOGGER.setLevel(level);
    }

    private static String getThread() {
        return Thread.currentThread().getName() + "-->";
    }

    private static String responseToPrettyFormat(JsonObject j) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(j);
        return prettyJson;
    }

}
