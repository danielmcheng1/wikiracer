package com.danielmcheng1.wikiracing;

import org.kohsuke.args4j.Option;

/**
 * This class handles the programs arguments.
 */
public class CommandLineValues {
    @Option(name = "-sourceTitle", required = false, usage = "Specify the source Wikipedia title")
    private String sourceTitle;
    @Option(name = "-destTitle", required = false, usage = "Specify the destination Wikipedia title")
    private String destTitle;
    @Option(name = "-timeout", required = false, usage = "Specify optional timeout(s) for crawler to stop (defaults to 60s)")
    private Long timeout;
    @Option(name = "-startREST", required = false, usage = "Specify optional flag as Y to start the REST service")
    private String startREST;

    public CommandLineValues() {
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public String getDestTitle() {
        return destTitle;
    }

    public Long getTimeout() {
        return timeout;
    }
    public String getStartREST() {
        return startREST;
    }
}
