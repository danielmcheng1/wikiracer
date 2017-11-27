package com.danielmcheng1.wikiracing;

import com.google.gson.Gson;

import static spark.Spark.get;

/**
 * This class exposes a REST interface to the crawler functionality.
 */
public class CrawlerService {
    public static void main(String[] args) {
        final CrawlerService crawlerService = new CrawlerService();
        get("/crawl/:sourceTitle/:destTitle", (request, response) -> {
            response.type("application/json");
            CrawlerResult crawlResult = crawlerService.getCrawl(request.params(":sourceTitle"), request.params(":destTitle"));
            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(crawlResult)));
        });
        get("/crawl/:sourceTitle/:destTitle/:timeoutSeconds", (request, response) -> {
            response.type("application/json");
            CrawlerResult crawlResult = crawlerService.getCrawl(request.params(":sourceTitle"), request.params(":destTitle"), Long.parseLong(request.params(":timeoutSeconds")));
            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(crawlResult)));
        });
    }

    public CrawlerResult getCrawl(String sourceTitle, String destTitle) {
        CrawlerController crawlerController = new CrawlerController(sourceTitle, destTitle, (long) 15);
        crawlerController.runCrawl();
        return crawlerController.getResult();
    }

    public CrawlerResult getCrawl(String sourceTitle, String destTitle, Long timeoutSeconds) {
        CrawlerController crawlerController = new CrawlerController(sourceTitle, destTitle, timeoutSeconds);
        crawlerController.runCrawl();
        return crawlerController.getResult();
    }
}