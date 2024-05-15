package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// TODO @Data注解
@Component
@ConfigurationProperties(prefix = "sky.alioss")
@Data // @Data注解是由Lombok库提供的，会生成getter、setter以及equals()、hashCode()、toString()等方法
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
