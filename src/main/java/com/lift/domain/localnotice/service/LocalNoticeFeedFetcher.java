package com.lift.domain.localnotice.service;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 지자체 게시판 RSS 피드를 조회해 표준화된 {@link RawFeedItem} 목록으로 변환한다.
 *
 * <p>지자체마다 CMS가 달라도 RSS 2.0/Atom 스펙 자체는 표준이므로, Rome 라이브러리로
 * 파싱하면 개별 파서를 지자체별로 만들 필요가 없다.
 * {@code com.lift.domain.lifetransition.service.Gov24CatalogClient}가 odcloud.kr
 * serviceList를 페이지 단위로 가져오는 것과 대응하는 역할이다.
 */
@Slf4j
@Component
public class LocalNoticeFeedFetcher {

    private static final int CONNECT_TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();
    private static final int READ_TIMEOUT_MS = (int) Duration.ofSeconds(15).toMillis();
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; LiftLocalNoticeBot/1.0)";

    /** RSS 항목이 HTML 태그를 포함한 요약을 줄 때가 많아, 태그만 제거한 평문으로 정리한다. */
    private static final java.util.regex.Pattern HTML_TAG = java.util.regex.Pattern.compile("<[^>]+>");

    public FetchResult fetch(String feedUrl, int maxItems) {
        try {
            URL url = URI.create(feedUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            try (InputStream input = connection.getInputStream()) {
                SyndFeed feed = new SyndFeedInput().build(new XmlReader(input));
                List<RawFeedItem> items = feed.getEntries().stream()
                        .limit(Math.max(1, maxItems))
                        .map(this::toRawItem)
                        .filter(Objects::nonNull)
                        .toList();
                return FetchResult.success(items);
            }
        } catch (Exception e) {
            log.warn("지자체 RSS 수집 실패. feedUrl={}", feedUrl, e);
            return FetchResult.failure(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private RawFeedItem toRawItem(SyndEntry entry) {
        String guid = StringUtils.hasText(entry.getUri()) ? entry.getUri() : entry.getLink();
        if (!StringUtils.hasText(guid)) {
            return null;
        }
        String title = entry.getTitle();
        if (!StringUtils.hasText(title)) {
            return null;
        }
        Instant publishedAt = toInstant(entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate());
        return new RawFeedItem(guid, title.trim(), entry.getLink(), stripHtml(description(entry)), publishedAt);
    }

    private String description(SyndEntry entry) {
        SyndContent content = entry.getDescription();
        return content == null ? null : content.getValue();
    }

    private String stripHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String plain = HTML_TAG.matcher(value).replaceAll(" ").replaceAll("&nbsp;", " ").trim();
        return plain.replaceAll("\\s+", " ");
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /** RSS 항목 하나를 표준화한 형태. */
    public record RawFeedItem(String guid, String title, String link, String summary, Instant publishedAt) {
    }

    /** 피드 하나 조회 결과. 성공/실패 여부와 원인을 함께 들고 다녀 소스별 장애를 기록할 수 있게 한다. */
    public record FetchResult(boolean success, List<RawFeedItem> items, String errorMessage) {
        public static FetchResult success(List<RawFeedItem> items) {
            return new FetchResult(true, items, null);
        }

        public static FetchResult failure(String errorMessage) {
            return new FetchResult(false, List.of(), errorMessage);
        }
    }
}
