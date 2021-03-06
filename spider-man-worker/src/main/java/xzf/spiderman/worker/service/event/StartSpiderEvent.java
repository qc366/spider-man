package xzf.spiderman.worker.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xzf.spiderman.common.event.Event;
import xzf.spiderman.worker.entity.SpiderCnf;
import xzf.spiderman.worker.entity.SpiderStore;
import xzf.spiderman.worker.service.SpiderKey;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartSpiderEvent implements Event
{
    private SpiderKey key;
    private SpiderCnf cnf;
    private List<SpiderStore> stores;

}
