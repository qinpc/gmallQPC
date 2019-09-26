package com.atguigu.gmall.user;

import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.user.mapper")
@ComponentScan("com.atguigu.gmall")
public class GmallUserManageApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmallUserManageApplication.class, args);
	}

}
