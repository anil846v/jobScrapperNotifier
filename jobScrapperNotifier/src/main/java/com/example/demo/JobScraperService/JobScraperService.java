package com.example.demo.JobScraperService;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Service;
import com.example.demo.model.Job;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobScraperService {
    private static final int MAX_DAYS_OLD = 3;
    private static final boolean FRESHER_ONLY = true;
    private static final int MAX_PAGES = 5; // Change as needed

    private static final Set<String> QA_KEYWORDS = Set.of(
            "qa", "tester", "quality assurance", "test engineer", "sdet",
            "automation tester", "manual tester", "selenium", "appium", "jira"
    );

    private static final Set<String> DEV_KEYWORDS = Set.of(
            "developer", "engineer", "programmer", "development",
            "full stack", "backend", "frontend",
            "java", "python", "javascript", "react", "angular", "spring", "node",
            "software", "junior", "trainee", "intern", "fresher", "web", "associate",
            "support engineer", "technical support", "application support",
            "product support engineer", "l1 support", "l2 support",
            "it support",
            "system engineer"
    );

    private static final Pattern POSTED_PATTERN = Pattern.compile(
            "(\\d+)?\\s*(day|days|hr|hrs|min|mins|just now)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXP_PATTERN = Pattern.compile(
            ".*\\b(0\\s*-\\s*\\d+|fresher|0\\s*yrs?).*",
            Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter TITLE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public List<Job> scrapeJobs() {
        List<String> urls = List.of(
            "https://www.naukri.com/qa-tester-jobs-in-india",
            "https://www.naukri.com/web-developer-jobs-in-india",
            "https://www.naukri.com/software-developer-jobs-in-india",
            "https://www.naukri.com/technical-support-jobs-in-india",
            "https://www.naukri.com/application-support-jobs-in-india",
            "https://www.naukri.com/l1-support-jobs-in-india"
        );

        List<Job> jobs = Collections.synchronizedList(new ArrayList<>());
        Set<String> seen = Collections.synchronizedSet(new HashSet<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Void>> futures = new ArrayList<>();
        
        for (String url : urls) {
            futures.add(executor.submit(() -> {
                scrapeMatchingPages(url, jobs, seen);
                return null;
            }));
        }
        
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        System.out.println("FINAL: " + jobs.size() + " jobs collected.");
        return jobs;
    }

    private void scrapeMatchingPages(String baseUrl, List<Job> jobs, Set<String> seen) {
        WebDriver driver = null;
        WebDriverWait wait = null;
        int pagesVisited = 0;

        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage",
                    "--disable-gpu", "--window-size=1920,1080", "--disable-images",
                     "--disable-plugins", "--disable-extensions",
                    "--user-agent=Mozilla/5.0");
            options.setPageLoadTimeout(Duration.ofSeconds(10));

            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            for (int page = 1; page <= MAX_PAGES; page++) {
                String pageUrl = (page == 1) ? baseUrl : baseUrl + "-" + page;
                pagesVisited++;
                System.out.println("Scraping page " + pagesVisited + ": " + pageUrl);

                driver.get(pageUrl);
                
                List<WebElement> jobCards;
                try {
                    jobCards = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.cssSelector("div.srp-jobtuple-wrapper")));
                } catch (TimeoutException e) {
                    System.out.println("Timeout waiting for job cards on " + pageUrl);
                    break;
                }

                if (jobCards.isEmpty()) {
                    System.out.println("No more jobs found on " + pageUrl + ", stopping pagination.");
                    break;
                }

                for (WebElement card : jobCards) {
                    try {
                        // Get all text once and cache it
                        String cardText = card.getText().toLowerCase();
                        if (cardText.contains("week") || cardText.contains("month")) {
                            continue;
                        }

                        // Batch DOM queries
                        WebElement titleEl = card.findElement(By.cssSelector("a.title"));
                        String title = titleEl.getText().trim();
                        String jobUrl = titleEl.getAttribute("href");
                        
                        if (!seen.add(jobUrl)) continue;

                        String titleLower = title.toLowerCase();
                        String company = getElementText(card, "a.comp-name, a.subTitle", "Unknown");
                        String location = getElementText(card, "span.locWdth", "India");
                        String skills = extractSkills(card);

                        // Early filtering
                        String fullText = titleLower + " " + skills;
                        if (!containsAny(fullText, QA_KEYWORDS) && !containsAny(fullText, DEV_KEYWORDS)) {
                            continue;
                        }

                        if (FRESHER_ONLY) {
                            String exp = getElementText(card, "span.expwd, span.exp", "");
                            if (!EXP_PATTERN.matcher(exp.toLowerCase()).matches()) {
                                continue;
                            }
                        }

                        LocalDateTime postedDateTime = extractPostedDate(card, cardText);
                        if (isOlderThanMaxDays(postedDateTime.toLocalDate())) {
                            continue;
                        }

                        Job job = new Job();
                        job.setTitle(title);
                        job.setCompany(company);
                        job.setLocation(location);
                        job.setUrl(jobUrl);
                        job.setPostedDate(postedDateTime.toLocalDate());
                        job.setSkills(skills);

                        jobs.add(job);

                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private String getElementText(WebElement parent, String selector, String defaultValue) {
        try {
            return parent.findElement(By.cssSelector(selector)).getText().trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String extractSkills(WebElement card) {
        try {
            List<WebElement> skillEls = card.findElements(By.cssSelector("ul.tag li"));
            if (skillEls.isEmpty()) return "Unknown";
            
            return skillEls.stream()
                    .map(el -> el.getText().trim().toLowerCase())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Unknown");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private LocalDateTime extractPostedDate(WebElement card, String cardText) {
        try {
            WebElement postedEl = card.findElement(By.cssSelector("span.ni-job-tuple-icon-srp-calendar"));
            String postedText = postedEl.getText().trim().toLowerCase();
            
            if (postedText.contains("week") || postedText.contains("month")) {
                return LocalDateTime.now().minusDays(MAX_DAYS_OLD + 1);
            }

            String titleAttr = postedEl.getAttribute("title");
            if (titleAttr != null && !titleAttr.isEmpty()) {
                return LocalDateTime.parse(titleAttr.trim(), TITLE_FORMATTER);
            }
            return parseRelativeTime(postedText);
        } catch (Exception e) {
            Matcher m = POSTED_PATTERN.matcher(cardText);
            String rel = m.find() ? m.group(0).toLowerCase() : "just now";
            return parseRelativeTime(rel);
        }
    }

    private boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private LocalDateTime parseRelativeTime(String text) {
        Matcher m = POSTED_PATTERN.matcher(text);
        if (!m.find()) return LocalDateTime.now();

        int val = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
        String unit = m.group(2).toLowerCase();

        LocalDateTime now = LocalDateTime.now();
        return switch (unit) {
            case "min", "mins" -> now.minusMinutes(val > 0 ? val : 1);
            case "hr", "hrs" -> now.minusHours(val > 0 ? val : 1);
            case "day", "days" -> now.minusDays(val > 0 ? val : 1);
            default -> now;
        };
    }

    private boolean isOlderThanMaxDays(LocalDate date) {
        return date.isBefore(LocalDate.now().minusDays(MAX_DAYS_OLD));
    }
}