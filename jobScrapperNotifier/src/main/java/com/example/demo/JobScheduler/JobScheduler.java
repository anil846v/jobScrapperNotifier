package com.example.demo.JobScheduler;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.JobFilterService.com.JobFilterService;
import com.example.demo.JobScraperService.JobScraperService;
import com.example.demo.NotificationService.NotificationService;
import com.example.demo.model.Job;



@Component
public class JobScheduler {

    @Autowired
    private JobScraperService scraper;

    @Autowired
    private JobFilterService filterService;

    @Autowired
    private NotificationService notifier;

    @Scheduled(fixedRate = 600000)  // every 10 minutes
    public void fetchJobs() {

        List<Job> scraped = scraper.scrapeJobs();
        List<Job> fresh = filterService.filterNewJobs(scraped);

        filterService.saveNewJobs(fresh);
        notifier.sendEmail(fresh);
        filterService.markAsNotified(fresh);

        System.out.println("Scraped: " + scraped.size());
        System.out.println("New Jobs: " + fresh.size());
    }
}
