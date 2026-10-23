package com.transit.SGComplaint;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan({"com.transit.SGComplaint.mapper", "com.transit.SGComplaint.repository"})
public class SGComplaintApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SGComplaintApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(SGComplaintApplication.class, args);
	}

}
