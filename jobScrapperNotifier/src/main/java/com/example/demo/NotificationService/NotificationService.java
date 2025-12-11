package com.example.demo.NotificationService;


//import java.net.URI;
//import java.net.URLEncoder;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.model.Job;


@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(List<Job> jobs) {

        if (jobs.isEmpty()) return;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo("settianilkumar846v@gmail.com");
        mail.setSubject("New QA/Software Jobs Found");

        StringBuilder body = new StringBuilder();

        for (Job j : jobs) {
            body.append("" + j.getTitle() + "\n")
                    .append("Company: " + j.getCompany() + "\n")
                    .append("Location: " + j.getLocation() + "\n")
                    .append("Skills: " + j.getSkills() + "\n")
                    .append("URL: " + j.getUrl() + "\n")
                    .append("Posted: " + j.getPostedDate() + "\n\n");
        }

        mail.setText(body.toString());
        mailSender.send(mail);
    }
}

//    // Telegram notification
//    public void sendTelegram(List<Job> jobs) {
//        if (jobs.isEmpty()) return;
//        String token = "YOUR_BOT_TOKEN";
//        String chatId = "YOUR_CHAT_ID";
//        for (Job job : jobs) {
//            try {
//                String msg = job.getTitle() + " - " + job.getCompany() + "\n" + job.getUrl();
//                String encodedMsg = URLEncoder.encode(msg, StandardCharsets.UTF_8);
//                String url = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + encodedMsg;
//                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
//                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    

