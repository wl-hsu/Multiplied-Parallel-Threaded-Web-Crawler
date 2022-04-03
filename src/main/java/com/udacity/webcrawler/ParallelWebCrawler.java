package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final List<Pattern> ignoredUrls;
  private final int maxDepth;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @MaxDepth int maxDepth,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls;
    this.maxDepth = maxDepth;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    final Map<String, Integer> counts = new ConcurrentHashMap<>();
    final ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
    final Instant deadline = clock.instant().plus(timeout);

    for (final String url : startingUrls) {
      if (clock.instant().isAfter(deadline)) {
        break;
      }

      final InternalCrawlerNode internalCrawlerNode = new InternalCrawlerNode.Builder()
              .setMaxDepth(maxDepth)
              .setClock(clock)
              .setDeadline(deadline)
              .setUrl(url)
              .setParserFactory(parserFactory)
              .setCounts(counts)
              .setVisitedUrls(visitedUrls)
              .setIgnoredUrls(ignoredUrls)
              .build();

      pool.invoke(internalCrawlerNode);
    }

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}

class InternalCrawlerNode extends RecursiveAction {
  private final int maxDepth;
  private final Clock clock;
  private final Instant deadline;
  private final String url;
  private final PageParserFactory parserFactory;
  private final Map<String, Integer> counts;
  private final ConcurrentSkipListSet<String> visitedUrls;
  private final List<Pattern> ignoredUrls;

  public InternalCrawlerNode(final int maxDepth, final Clock clock, final Instant deadline,
                             final String url, final PageParserFactory parserFactory,
                             final Map<String, Integer> counts, final ConcurrentSkipListSet<String> visitedUrls,
                             final List<Pattern> ignoredUrls) {
    this.maxDepth = maxDepth;
    this.clock = clock;
    this.deadline = deadline;
    this.url = url;
    this.parserFactory = parserFactory;
    this.counts = counts;
    this.visitedUrls = visitedUrls;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  protected void compute() {
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return;
    }

    for (final Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return;
      }
    }

    if (!visitedUrls.add(url)) {
      return;
    }

    final PageParser.Result result = parserFactory.get(url).parse();
    countWordsInUrl(result, counts);

    final List<InternalCrawlerNode> subTasks = result.getLinks()
            .stream()
            .map(link -> new InternalCrawlerNode.Builder()
                    .setMaxDepth(maxDepth - 1)
                    .setClock(clock)
                    .setDeadline(deadline)
                    .setUrl(link)
                    .setParserFactory(parserFactory)
                    .setCounts(counts)
                    .setVisitedUrls(visitedUrls)
                    .setIgnoredUrls(ignoredUrls)
                    .build())
            .collect(Collectors.toList());

    invokeAll(subTasks);
  }

  public static void countWordsInUrl(final PageParser.Result result, final Map<String, Integer> counts) {
    for (final Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
      counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : counts.get(e.getKey()) + e.getValue());
    }
  }

  public static final class Builder {
    private int maxDepth;
    private Clock clock;
    private Instant deadline;
    private String url;
    private PageParserFactory parserFactory;
    private Map<String, Integer> counts;
    private ConcurrentSkipListSet<String> visitedUrls;
    private List<Pattern> ignoredUrls;

    public Builder setMaxDepth(final int maxDepth) {
      this.maxDepth = maxDepth;
      return this;
    }

    public Builder setClock(final Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder setDeadline(final Instant deadline) {
      this.deadline = deadline;
      return this;
    }

    public Builder setUrl(final String url) {
      this.url = url;
      return this;
    }

    public Builder setParserFactory(final PageParserFactory parserFactory) {
      this.parserFactory = parserFactory;
      return this;
    }

    public Builder setCounts(final Map<String, Integer> counts) {
      this.counts = counts;
      return this;
    }

    public Builder setVisitedUrls(final ConcurrentSkipListSet<String> visitedUrls) {
      this.visitedUrls = visitedUrls;
      return this;
    }

    public Builder setIgnoredUrls(final List<Pattern> ignoredUrls) {
      this.ignoredUrls = ignoredUrls;
      return this;
    }

    public InternalCrawlerNode build() {
      return new InternalCrawlerNode(maxDepth, clock, deadline, url,
              parserFactory, counts, visitedUrls, ignoredUrls);
    }
  }
}