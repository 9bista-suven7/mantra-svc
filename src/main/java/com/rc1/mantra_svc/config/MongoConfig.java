package com.rc1.mantra_svc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

/**
 * MongoDB configuration — enables reactive auditing for
 * {@code @CreatedDate} and {@code @LastModifiedDate} fields.
 */
@Configuration
@EnableReactiveMongoAuditing
public class MongoConfig {
}
