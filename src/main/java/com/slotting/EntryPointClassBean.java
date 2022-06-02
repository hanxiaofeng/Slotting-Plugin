package com.slotting;

import java.util.ArrayList;
import java.util.List;

public class EntryPointClassBean {

    Long id = -1L;
    /**
     * class名字，包含包名。
     * - 例如 ：com.dboy.slotting.data.EntryPointClassBean
     */
    String classPath = "";
    /**
     * 代码切入点，当前[classPath]需要埋点的方法信息。
     */
    List<EntryPointMethodBean> entryPoints;
}
