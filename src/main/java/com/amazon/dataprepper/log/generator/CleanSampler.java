package com.amazon.dataprepper.log.generator;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.*;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class CleanSampler extends AbstractJavaSamplerClient implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanSampler.class);

    private static final String LOG_URI_PATH = "/log/ingest";
    private static final String SSL = "ssl";
    private static final String TARGET_ADDRESS = "targetAddress";
    private static final String URI_PATH = "uri";
    private static final String TIMEOUT_MS = "timeoutMs";
    private static final String TARGET_PORT = "targetPort";
    private static final String NUM_LOGS = "numLogs";
    private final ApacheLogGenerator apacheLogGenerator = new ApacheLogGenerator();

    int timeoutMilliseconds = 10000;

    WebClient webClient;
    String targetWithPort;
    String uri;
    SessionProtocol sessionProtocol;
    int numLogs = 20;

    public void setupTest(JavaSamplerContext javaSamplerContext) {
        String targetAddress = javaSamplerContext.getParameter(TARGET_ADDRESS);
        String targetPort = javaSamplerContext.getParameter(TARGET_PORT);
        boolean ssl = Boolean.parseBoolean(javaSamplerContext.getParameter(SSL));
        uri = javaSamplerContext.getParameter(URI_PATH, "/");
        targetWithPort = targetAddress + ":" + targetPort;

        String timeoutMs = javaSamplerContext.getParameter(TIMEOUT_MS);
        this.timeoutMilliseconds = Integer.parseInt(timeoutMs);

        if (ssl) {
            webClient = WebClient.builder("https://" + targetWithPort)
                    .factory(ClientFactory.insecure())
                    .responseTimeoutMillis(timeoutMilliseconds)
                    .build();
            sessionProtocol = SessionProtocol.HTTPS;
        } else {
            webClient = WebClient.builder("http://" + targetWithPort)
                    .responseTimeoutMillis(timeoutMilliseconds)
                    .build();
            sessionProtocol = SessionProtocol.HTTP;
        }

        String numLogsArg = javaSamplerContext.getParameter(NUM_LOGS);
        if (!StringUtils.isEmpty(numLogsArg)) {
            numLogs = Integer.valueOf(numLogsArg);
        }
    }

    @Override
    public Arguments getDefaultParameters() {

        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(TARGET_ADDRESS, "localhost");
        defaultParameters.addArgument(URI_PATH, LOG_URI_PATH);
        defaultParameters.addArgument(SSL, "false");
        defaultParameters.addArgument(TIMEOUT_MS, "10000");
        defaultParameters.addArgument(TARGET_PORT, "2021");
        defaultParameters.addArgument(NUM_LOGS, "20");

        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {

        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();

        try {
            final HttpData httpData = apacheLogGenerator.generateRandomApacheLogHttpData(numLogs);

            HttpResponse response = webClient.execute(RequestHeaders.builder()
                    .scheme(sessionProtocol)
                    .authority(targetWithPort)
                    .method(HttpMethod.POST)
                    .path(uri)
                    .contentType(MediaType.JSON_UTF_8)
                    .build(), httpData);

            response.aggregate().get();

            sampleResult.sampleEnd();
            sampleResult.setSuccessful(Boolean.TRUE);
            sampleResult.setResponseCodeOK();
        } catch (Exception e) {
            LOGGER.error("Request was not successfully processed", e);
            sampleResult.sampleEnd();
            sampleResult.setResponseMessage(e.getMessage());
            sampleResult.setSuccessful(Boolean.FALSE);
        }

        return sampleResult;
    }
}
