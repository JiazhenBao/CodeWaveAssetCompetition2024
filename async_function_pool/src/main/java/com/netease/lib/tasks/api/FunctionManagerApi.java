package com.netease.lib.tasks.api;

import com.netease.lib.tasks.model.ThreadResultDTO;
import com.netease.lowcode.core.annotation.NaslLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;

/**
 * @Author baojz
 * @Date: 2024/06/11
 * @description 并发任务管理
 */
@Component
public class FunctionManagerApi {

    private static final Logger logger = LoggerFactory.getLogger("LCAP_CUSTOMIZE_LOGGER");

    /**
     * 逻辑关键词，逻辑
     */
    private final Map<String, Function<String, String>> functionRegister = new ConcurrentHashMap<>();
    /**
     * 运行中任务注册
     * 任务id，任务
     */
    private final Map<String, CompletableFuture<String>> runningTaskRegister = new ConcurrentHashMap<>();


    @Resource(name = "libraryCommonTaskExecutor")
    private Executor contextAwareExecutor;

    /**
     * 初始化注册逻辑
     *
     * @param logicKey        逻辑关键词
     * @param registeredlogic 逻辑
     * @return
     */
    @NaslLogic
    public Boolean registerLogic(String logicKey, Function<String, String> registeredlogic) {
        functionRegister.put(logicKey, registeredlogic);
        logger.info("registerLogicInit success: {}", logicKey);
        return true;
    }


    /**
     * 并发执行逻辑
     *
     * @param logicKey   逻辑关键词
     * @param requestStr 逻辑的入参
     * @return
     */
    @NaslLogic
    public String asyncRunLogic(String logicKey, String requestStr) {
        Function<String, String> function = functionRegister.get(logicKey);
        if (function == null) {
            logger.error("asyncRunLogic not exist: {}", logicKey);
            return null;
        }
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> function.apply(requestStr), contextAwareExecutor);
        logger.info("asyncRunLogic success: {}", logicKey);
        String taskId = UUID.randomUUID().toString();
        runningTaskRegister.put(taskId, future);
        return taskId;
    }

    /**
     * 同步获取结果
     *
     * @param taskIdList 执行的任务id列表
     * @return
     */
    @NaslLogic
    public List<ThreadResultDTO> syncGetLogicResult(List<String> taskIdList) {
        List<ThreadResultDTO> resultDTOList = new ArrayList<>();
        taskIdList.forEach(taskId -> {
            ThreadResultDTO resultDTO = new ThreadResultDTO();
            resultDTO.setTaskId(taskId);
            CompletableFuture<String> functionWeak = runningTaskRegister.get(taskId);
            if (functionWeak == null) {
                resultDTO.setTaskStatus(3);
                resultDTOList.add(resultDTO);
                return;
            }
            CompletableFuture future = runningTaskRegister.get(taskId);
            String result = (String) future.join();
            runningTaskRegister.remove(taskId);
            resultDTO.setTaskResult(result);
            resultDTO.setTaskStatus(1);
            resultDTOList.add(resultDTO);
        });
        return resultDTOList;
    }

    /**
     * 异步获取结果
     *
     * @param taskIdList 执行的任务id列表
     * @return
     */
    @NaslLogic
    public List<ThreadResultDTO> asyncGetLogicResult(List<String> taskIdList) {
        List<ThreadResultDTO> resultDTOList = new ArrayList<>();
        taskIdList.forEach(taskId -> {
            ThreadResultDTO resultDTO = new ThreadResultDTO();
            resultDTO.setTaskId(taskId);
            CompletableFuture<String> functionWeak = runningTaskRegister.get(taskId);
            if (functionWeak == null) {
                resultDTO.setTaskStatus(3);
                resultDTOList.add(resultDTO);
                return;
            }
            CompletableFuture future = runningTaskRegister.get(taskId);
            if (future.isDone()) {
                String result = (String) future.join();
                runningTaskRegister.remove(taskId);
                resultDTO.setTaskResult(result);
                resultDTO.setTaskStatus(1);
            } else {
                resultDTO.setTaskStatus(2);
            }
            resultDTOList.add(resultDTO);
        });
        return resultDTOList;
    }

    /**
     * 异步执行任务，无返回结果
     */
    @NaslLogic
    public Boolean asyncRunLogicNoResult(Function<String, String> asyncfunction, String requestStr) {
        try {
            contextAwareExecutor.execute(() -> asyncfunction.apply(requestStr));
            return true;
        } catch (RejectedExecutionException e) {
            logger.error("Async task rejected for request: {}", requestStr, e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to submit async task for request: {}", requestStr, e);
            return false;
        }
    }

}