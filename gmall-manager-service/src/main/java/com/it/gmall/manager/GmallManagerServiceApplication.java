package com.it.gmall.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.it.gmall.manager.mapper")
/*开启声明式事务*/
@EnableTransactionManagement
@ComponentScan(basePackages = "com.it.gmall")
public class GmallManagerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallManagerServiceApplication.class, args);
    }

}
