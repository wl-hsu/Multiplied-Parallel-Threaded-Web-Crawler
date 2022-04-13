# Multiplied Parallel Threaded Web Crawler

This is a completed multiped parallel threaded web crawler project.
The starting code is assumed the legacy web crawler in a specific company is a single-threaded version. We need to improve its performance with advanced features like multi-threading, asynchronous, synchronous, reflection, design patterns,  I/O stream(json file), and AOP (aspect orientation program).

The starting source come from https://github.com/udacity/cd0381-advanced-java-programming-techniques-projectstarter

## Getting Started

### Dependencies

  * Java 11 or higher
  * Maven 3.6.3 or higher
  * IntelliJ IDEA

### Run the Parallel Crawler

#### step 1

Find the config folder and edit the document as required.

```
{
  "startPages": ["http://example.com", "http://example.com/foo"],
  "ignoredUrls": ["http://example\\.com/.*"],
  "ignoredWords": ["^.{1,3}$"],
  "parallelism": 4,
  "implementationOverride": "com.udacity.webcrawler.SequentialWebCrawler",
  "maxDepth": 10,
  "timeoutSeconds": 2,
  "popularWordCount": 3,
  "profileOutputPath": "profileData.txt"
  "resultPath": "crawlResults.json"
}
```
  * `startPages` - These URLs are the starting point of the web crawl.
  
  * `ignoredUrls` - A list of regular expressions defining which, if any, URLs should not be followed by the web crawler. In this example, the second starting page will be ignored.
  
  * `ignoredWords` - A list of regular expressions defining which words, if any, should not be counted toward the popular word count. In this example, words with 3 or fewer characters are ignored.
  
  * `parallelism` - The desired parallelism that should be used for the web crawl. If set to 1, the legacy crawler should be used. If less than 1, parallelism should default to the number of cores on the system.
  
  * `implementationOverride` - An explicit override for which web crawler implementation should be used for this crawl. In this example, the legacy crawler will always be used, regardless of the value of the "parallelism" option.

  If this option is empty or unset, the "parallelism" option will be used (instead of the "implementationOverride" option) to determine which crawler to use. If this option is set to a non-empty string that is not the fully-qualified name of a class that implements the `WebCrawler` interface, the crawler will immediately fail.
  
  * `maxDepth` - The max depth of the crawl. The "depth" of a crawl is the maximum number of links the crawler is allowed to follow from the starting pages before it must stop. This option can be used to limit how far the crawler drifts from the starting URLs, or can be set to a very high number if that doesn't matter.

#### step 2

After edit the document as required, it should be able to run it with the following commands:

```
mvn package
java -classpath target/udacity-webcrawler-1.0.jar \
    com.udacity.webcrawler.main.WebCrawlerMain \
    src/main/config/sample_config.json
```
