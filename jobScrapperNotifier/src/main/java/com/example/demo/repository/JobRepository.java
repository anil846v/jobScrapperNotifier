package com.example.demo.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Job;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByUrl(String url);
}
