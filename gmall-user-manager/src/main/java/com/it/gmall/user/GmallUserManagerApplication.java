package com.it.gmall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.it.gmall.user.mapper")
/*redis扫描包*/
@ComponentScan(basePackages = "com.it.gmall")
public class GmallUserManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GmallUserManagerApplication.class, args);
    }
}
