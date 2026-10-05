package com.modernclinic.patientmanagement.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore getFirestore() throws IOException {
        // Check if Firebase is already initialized to avoid "app already exists" errors
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount =
                    getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");

            if (serviceAccount == null) {
                throw new IOException("Could not find serviceAccountKey.json in src/main/resources!");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase initialized successfully in the Bean method!");
        }

        return FirestoreClient.getFirestore();
    }
}