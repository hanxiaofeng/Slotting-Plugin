package com.slotting;

import java.util.HashMap;

public class EntryPointMethodBean {

    /**
     * emmm这个暂时保留，没啥用
     */
    Long id = -1L;
    /**
     * 方法名字
     */
    String methodName = "";
    /**
     * 事件信息，多个事件通过英文 [,] 逗号进行分割
     * - event:"msg,msg2,msg3"
     */
    String event = "";
    /**
     * 具有key->value映射关系的事件
     */
    HashMap<String, Object> eventMap = null;
    /**
     * 是否在方法[methodName]第一行插入，false为在方法return的位置和结束的位置插入。
     */
    Boolean isFirstLine = false;
}
