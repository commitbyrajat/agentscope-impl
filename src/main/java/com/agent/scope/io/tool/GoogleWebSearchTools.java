package com.agent.scope.io.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GoogleWebSearchTools {

    private static final Logger log = LoggerFactory.getLogger(GoogleWebSearchTools.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Tool(description = "Searches google.com directly and uses Apache Tika to extract page snippets and extracted hyperlinks.")
    public String webSearch(
            @ToolParam(name = "query", description = "The search query keywords") String query,
            @ToolParam(name = "maxResults", description = "Maximum number of links and snippets to extract (1 to 5)") int maxResults
    ) {
        log.info("Executing Google Tika Search Tool for query: {}", query);

        try {
            int limit = Math.clamp(maxResults, 1, 5);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://www.google.com/search?q=" + encodedQuery + "&hl=en";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                return "Failed to fetch google.com. HTTP code: " + response.statusCode();
            }

            TikaLinkAndTextHandler handler = new TikaLinkAndTextHandler(limit);

            try (InputStream input = new ByteArrayInputStream(response.body())) {
                HtmlParser parser = new HtmlParser();
                parser.parse(input, handler, new Metadata(), new ParseContext());
            }

            return handler.buildFormattedSummary(query);

        } catch (Exception e) {
            log.error("Tika search parsing failed", e);
            return "Failed to execute Google search: " + e.getMessage();
        }
    }

    private static class TikaLinkAndTextHandler extends DefaultHandler {
        private final int maxLinks;
        private final StringBuilder fullText = new StringBuilder();
        private final StringBuilder currentAnchorText = new StringBuilder();
        private String currentHref = null;
        private final Map<String, String> extractedLinks = new LinkedHashMap<>();

        public TikaLinkAndTextHandler(int maxLinks) {
            this.maxLinks = maxLinks;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("a".equalsIgnoreCase(qName)) {
                String href = attributes.getValue("href");
                if (href != null && href.startsWith("http") && !href.contains("google.com")) {
                    this.currentHref = href;
                    this.currentAnchorText.setLength(0);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            String text = new String(ch, start, length);
            fullText.append(text);
            if (currentHref != null) {
                currentAnchorText.append(text);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("a".equalsIgnoreCase(qName) && currentHref != null) {
                String anchorTitle = currentAnchorText.toString().trim();
                if (!anchorTitle.isEmpty() && extractedLinks.size() < maxLinks) {
                    extractedLinks.putIfAbsent(currentHref, anchorTitle);
                }
                currentHref = null;
                currentAnchorText.setLength(0);
            }
        }

        public String buildFormattedSummary(String query) {
            StringBuilder sb = new StringBuilder();
            sb.append("Google Search Results for '").append(query).append("':\n\n");

            if (!extractedLinks.isEmpty()) {
                sb.append("Extracted Links & Titles:\n");
                int idx = 1;
                for (Map.Entry<String, String> entry : extractedLinks.entrySet()) {
                    sb.append(idx++).append(". ").append(entry.getValue()).append("\n")
                            .append("   URL: ").append(entry.getKey()).append("\n");
                }
                sb.append("\n");
            }

            String cleanBody = fullText.toString()
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n\\s*\\n+", "\n")
                    .trim();

            if (cleanBody.length() > 2000) {
                cleanBody = cleanBody.substring(0, 2000) + "... [truncated]";
            }

            sb.append("Extracted Content Snippet:\n").append(cleanBody);
            return sb.toString();
        }
    }
}