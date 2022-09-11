package com.gongdel.microservices.core.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan("com.gongdel")
public class ReviewServiceApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

	private final Integer connectionPoolSize;

	@Autowired
	public ReviewServiceApplication(
			@Value("${spring.datasource.maximum-pool-size:10}")
					Integer connectionPoolSize
	) {
		this.connectionPoolSize = connectionPoolSize;
	}


	/**
	 * ㅡMysql을 사용하고 있는, review 서비스는 블로킹 모델을 사용한다
	 * 스케줄러는 일정 수의 스레드를 보유한 전용 스레드풀의 스레드에서 블로킹 코드를 실행한다.
	 * 스레드풀을 사용해 블로킹 코드를 처리하면 마이크로 서비스에서 사용할 스레드의 고갈을 방지하므로,
	 * 마이크로서비스의 논블로킹 처리에 영향을 주지 않는다.
	 */
	@Bean
	public Scheduler jdbcScheduler() {
		LOG.info("Creates a jdbcScheduler with connectionPoolSize = " + connectionPoolSize);
		return Schedulers.fromExecutor(Executors.newFixedThreadPool(connectionPoolSize));
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);

		String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
		LOG.info("Connected to MySQL: " + mysqlUri);
	}
}
