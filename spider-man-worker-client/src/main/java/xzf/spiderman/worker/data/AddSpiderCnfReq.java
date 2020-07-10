package xzf.spiderman.worker.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Data
@NoArgsConstructor
@ToString
public class AddSpiderCnfReq
{
    @NotBlank(message = "ID不能为空")
    private String id ;

    @NotBlank(message = "分组不能为空")
    private String groupId;

    @NotBlank(message = "服务器不能为空")
    private String serverId;

    @NotBlank(message = "保存数据源不能为空")
    private String storeId;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "类型不能为空")
    private Integer type;

    private String params;

    private String desc;

    @NotBlank(message = "爬虫处理程序不能为空")
    private String processor;

    @NotBlank(message = "停止条件计数器不能为空")
    private Integer stopConditionCount;

}
