package com.example.demo.JobFilterService.com;


				

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Job;
import com.example.demo.repository.JobRepository;



@Service
public class JobFilterService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> filterNewJobs(List<Job> jobs) {
        List<Job> newJobs = new ArrayList<>();

        for (Job job : jobs) {
            if (jobRepository.findByUrl(job.getUrl()).isEmpty()) {
                newJobs.add(job);
            }
        }
        return newJobs;
    }

    public void saveNewJobs(List<Job> jobs) {
        jobRepository.saveAll(jobs);
    }

    public void markAsNotified(List<Job> jobs) {
        for (Job job : jobs) {
            job.setNotified(true);
            jobRepository.save(job);
        }
    }
}
