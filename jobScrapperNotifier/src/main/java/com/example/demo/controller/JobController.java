//package com.example.demo.controller;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.JobFilterService.com.JobFilterService;
//import com.example.demo.JobScraperService.JobScraperService;
//import com.example.demo.NotificationService.NotificationService;
//import com.example.demo.model.Job;
//import com.example.demo.repository.JobRepository;
//
//@RestController
//@RequestMapping("/api/jobs")
//public class JobController {
//
//    @Autowired
//    private JobScraperService scraper;
//
//    @Autowired
//    private JobFilterService filterService;
//
//    @Autowired
//    private NotificationService notifier;
//
//    @Autowired
//    private JobRepository jobRepository;
//
//
//    // ✅ 1. Scrape jobs (WITHOUT saving)
//    @GetMapping("/scrape")
//    public List<Job> scrapeJobs() {
//        return scraper.scrapeJobs();
//    }
//
//
//    // ✅ 2. Scrape → filter new → save to DB
//    @PostMapping("/save-new")
//    public List<Job> saveNewJobs() {
//        List<Job> scraped = scraper.scrapeJobs();
//        return filterService.filterNewJobs(scraped);
//    }
//
//
//    // ✅ 3. Get all jobs from DB
//    @GetMapping("/all")
//    public List<Job> getAllJobs() {
//        return jobRepository.findAll();
//    }
//
//
//    // ✅ 4. Get only NOT YET NOTIFIED jobs
//    @GetMapping("/new")
//    public List<Job> getOnlyNewJobs() {
//        return jobRepository.findAll().stream()
//                .filter(job -> !job.isNotified())
//                .toList();
//    }
//
//
//    // ✅ 5. Manually send email for un-notified jobs
//    @PostMapping("/notify")
//    public String sendEmailForNewJobs() {
//        List<Job> newJobs = jobRepository.findAll().stream()
//                .filter(job -> !job.isNotified())
//                .toList();
//
//        notifier.sendEmail(newJobs);
//        filterService.markAsNotified(newJobs);
//
//        return "Email sent for " + newJobs.size() + " jobs!";
//    }
//
//
//    // ✅ 6. FULL CYCLE: Scrape → Filter → Save → Notify (same as scheduler)
//    @PostMapping("/run-full-cycle")
//    public String runFullCycle() {
//        List<Job> scraped = scraper.scrapeJobs();
//        List<Job> newJobs = filterService.filterNewJobs(scraped);
//
//        notifier.sendEmail(newJobs);
//        filterService.markAsNotified(newJobs);
//
//        return "Full cycle executed. New jobs: " + newJobs.size();
//    }
//}
