package com.teambind.springproject;

import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestRedisConfig.class, TestKafkaConfig.class})
class SpringProjectApplicationTests {
	
	@Test
	void contextLoads() {
	}
	
}
