package org.dromara.sms4j.solon.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.smsProxy.SmsInvocationHandler;
import org.dromara.sms4j.comm.config.SmsBanner;
import org.dromara.sms4j.comm.config.SmsConfig;
import org.dromara.sms4j.comm.config.SmsSqlConfig;
import org.dromara.sms4j.comm.constant.Constant;
import org.dromara.sms4j.comm.delayedTime.DelayedTime;
import org.dromara.sms4j.comm.factory.BeanFactory;
import org.dromara.sms4j.core.SupplierSqlConfig;
import org.dromara.sms4j.solon.aop.SolonRestrictedProcess;
import org.dromara.sms4j.solon.utils.RedisUtils;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Props;

import java.util.concurrent.Executor;


@Slf4j
@Configuration
public class SmsAutowiredConfig {

    public static AopContext aopContext;

    private <T> T injectObj(String prefix, T obj) {
        //@Inject 只支持在字段、参数、类型上注入
        Props props = Solon.cfg().getProp(prefix);
        Utils.injectProperties(obj, props);
        return obj;
    }

    @Bean
    public SmsSqlConfig smsSqlConfig() {
        return injectObj("sms.sql", BeanFactory.getSmsSqlConfig());
    }

    @Bean
    public SmsConfig smsConfig(){
        SmsConfig smsConfig = injectObj("sms", BeanFactory.getSmsConfig());

        smsConfigCheck();

        return smsConfig;
    }

    /** 注入一个定时器*/
    @Bean
    public DelayedTime delayedTime(){
      return BeanFactory.getDelayedTime();
    }

    /** 注入线程池*/
    @Bean("smsExecutor")
    public Executor taskExecutor(@Inject SmsConfig config){
       return BeanFactory.setExecutor(config);
    }


    /** smsConfig参数意义为确保注入时smsConfig已经存在*/
    @Bean
    @Condition(onProperty ="${sms.config-type}=config_file")
    public SupplierConfig supplierConfig(@Inject SmsConfig smsConfig){
        return new SupplierConfig();
    }

    @Bean
    @Condition(onProperty ="${sms.config-type}=sql_config")
    public SupplierSqlConfig supplierSqlConfig(@Inject SmsSqlConfig smsSqlConfig){
        return new SupplierSqlConfig();
    }


    private void smsConfigCheck(){
        /* 如果配置中启用了redis，则注入redis工具*/
        if (BeanFactory.getSmsConfig().getRedisCache()){
            Solon.context().wrapAndPut(RedisUtils.class);
            SmsInvocationHandler.setRestrictedProcess(new SolonRestrictedProcess(aopContext));
            log.debug("The redis cache is enabled for sms4j");
        }

        //打印banner
        if (BeanFactory.getSmsConfig().getIsPrint()){
            SmsBanner.PrintBanner(Constant.VERSION);
        }
    }
}
