package xzf.spiderman.worker.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import xzf.spiderman.common.event.Event;
import xzf.spiderman.common.event.EventListener;
import xzf.spiderman.common.exception.BizException;
import xzf.spiderman.worker.configuration.HessianRedisTemplate;
import xzf.spiderman.worker.entity.SpiderCnf;
import xzf.spiderman.worker.service.event.CloseSpiderEvent;
import xzf.spiderman.worker.service.event.StartSpiderEvent;
import xzf.spiderman.worker.webmagic.BlockingPollRedisScheduler;
import xzf.spiderman.worker.webmagic.WorkerSpider;
import xzf.spiderman.worker.webmagic.WorkerSpiderLifeCycleListener;

import java.util.concurrent.TimeUnit;

import static xzf.spiderman.worker.configuration.WorkerConst.ZK_SPIDER_TASK_BASE_PATH;

/**
 * 1.应该zk上创建临时节点，把本爬虫信息设置到data
 *
 * 2.维护一条线程，管理我们的WorkerSpider，他的start/stop方法，由Boss来通过http调用
 *
 * 3.通过WorkerSpider的listener坚挺 canClose事件
 *   当 canClose为真的时候。 更新zk节点，设置为completed
 *
 */
@Service
@Slf4j
public class SpiderSlave implements EventListener
{
    private final CuratorFramework curator;
    private final WorkerSpiderFactory factory;


    @Autowired
    public SpiderSlave(CuratorFramework curator,WorkerSpiderFactory factory)
    {
        this.curator = curator;
        this.factory = factory;
    }


    public class StartSpiderHandler
    {
        private final SpiderKey key;
        private final SpiderCnf cnf;

        public StartSpiderHandler(SpiderKey key, SpiderCnf cnf) {
            this.key = key;
            this.cnf = cnf;
        }

        public void handle()
        {
            // 1. Build WorkerSpider
            WorkerSpiderLifeCycleListener listener = new WorkerSpiderLifeCycleListener(){
                @Override
                public void onBeforeStart(WorkerSpider spider) {
                    //  write the task data into zk
                    updateRunningTaskToZk();
                }

                @Override
                public void onCanCloseCondition(WorkerSpider spider) {
                    //  write the task data into zk
                    updateCanCloseTaskToZk(key);
                }
            };
            WorkerSpider spider = factory.create(cnf, listener);


            // 异步执行 start spider.


        }


        private void updateRunningTaskToZk()
        {
            String path = taskPath(key);
            byte[] data = JSON.toJSONBytes(SpiderTask.newRunningTask(key));

            try {
                curator.create().withMode(CreateMode.EPHEMERAL).forPath(path, data);
                log.info("Slave: task created. path="+path);
            } catch (Exception e) {
                throw new BizException("fail to updateRunningTaskToZk. " +e.getMessage(), e);
            }

        }
    }



    public class CloseSpiderHandler
    {
        private final SpiderKey key;
        private final SpiderCnf cnf;

        public CloseSpiderHandler(SpiderKey key, SpiderCnf cnf) {
            this.key = key;
            this.cnf = cnf;
        }

        public void handle()
        {
            // 1. Build WorkerSpider

            // 2. WorkerSpider start.

            // 3. write the task data into zk
            String path = taskPath(key);
            byte[] data = JSON.toJSONBytes(SpiderTask.newClosedTask(key));

            try {
                curator.setData().forPath(path, data);
            } catch (Exception exception) {
                // TODO ..
                exception.printStackTrace();
            }

            log.info("Slave: closed. path="+path);
        }
    }


    private void updateCanCloseTaskToZk(SpiderKey key)
    {
        try {
            String path = taskPath(key);
            byte[] closedData = JSON.toJSONBytes(SpiderTask.newCanCloseTask(key));
            curator.setData().forPath(path, closedData);
            log.info("Slave: task can close. path="+path);
        } catch (Exception e) {
            throw new BizException("fail to updateCanCloseTaskToZk. " +e.getMessage(), e);
        }
    }



    private String taskPath(SpiderKey key)
    {
        String path = ZK_SPIDER_TASK_BASE_PATH + "/" + key.getGroupId()+"/"+key.getSpiderId()+"/"+key.getCnfId();
        return path;
    }





    @Override
    public boolean supportEventType(Class<? extends Event> clazz)
    {
        return StartSpiderEvent.class.equals(clazz) || CloseSpiderEvent.class.equals(clazz);
    }


    @Override
    public void onEvent(Event event)
    {
        if(event instanceof StartSpiderEvent){
            onStartSpiderEvent( (StartSpiderEvent) event );
        }

        if(event instanceof CloseSpiderEvent){
            onCloseSpiderEvent( (CloseSpiderEvent) event );
        }
    }

    private void onCloseSpiderEvent(CloseSpiderEvent event)
    {
        CloseSpiderHandler h = new CloseSpiderHandler(event.getKey(), event.getCnf());
        h.handle();
    }

    private void onStartSpiderEvent(StartSpiderEvent event)
    {
        StartSpiderHandler h = new StartSpiderHandler(event.getKey(), event.getCnf());
        h.handle();
    }
}
