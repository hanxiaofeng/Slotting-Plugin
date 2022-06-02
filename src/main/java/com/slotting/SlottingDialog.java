package com.slotting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.find.FindManager;
import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.Processor;
import com.sun.istack.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;

public class SlottingDialog extends JDialog {
    private final Gson gson;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList listView;
    private JPanel contentPanel;

    Dimension screensize;

    int width = 550;
    int height = 300;
    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;

    private PsiFile slottingPsiFile;
    private JTextArea jtaSingleArea;

    public SlottingDialog(AnActionEvent event) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        gson = new Gson();

        setTitle("埋点助手"); // 设置title
        setSize(width, height); // 设置窗口大小

        // 设置窗口位置
        screensize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screensize.width - width) / 2, (screensize.height - height) / 2, width, height);

        initGlobalIntellij(event);

        boolean noError = addTopItem(event);
        if(!noError){
            onCancel();
            return;
        }

        //单个事件输入
        addInputSingleEvent();

        //map多事件输入
//        addInputMultipleEvent(event);
        addMultipleItem(true);

        recoverPointData();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setVisible(true);
    }

    private void addMultipleItem(boolean isFirst){

        JPanel jSingleEventPanel=new JPanel();
        JLabel jLabel = new JLabel();
        jLabel.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabel.setText(isFirst?"Event Map：":"           -->  ");

        JTextArea jtaKey = createJTextArea();
        JTextArea jtaEvent = createJTextArea();
        JLabel jLabelMethod = new JLabel();
        jLabelMethod.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabelMethod.setText(" : ");

        jSingleEventPanel.add(jLabel);
        jSingleEventPanel.add(jtaKey);
        jSingleEventPanel.add(jLabelMethod);
        jSingleEventPanel.add(jtaEvent);

        if(isFirst){
            JButton jButton = addEventBtn();
            jSingleEventPanel.add(jButton);
        }else {
            height += 30;
            setSize(width, height); // 设置窗口大小
        }

        contentPanel.add(jSingleEventPanel);
    }

    private JButton addEventBtn(){
        JButton jButton = new JButton("Add");
        ActionListener listener = new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                addMultipleItem(false);
            }
        };
        jButton.addActionListener(listener);
        return jButton;
    }

    private JTextArea createJTextArea() {
        JTextArea jtaEvent=new JTextArea("",1,8);
        jtaEvent.setCaret(new MyTextAreaCaret());
        jtaEvent.setLineWrap(true);    //设置文本域中的文本为自动换行
        jtaEvent.setFont(new Font("楷体",Font.BOLD,15));    //修改字体样式
        jtaEvent.setBorder(BorderFactory.createMatteBorder(
                1, 1, 1, 1, JBColor.WHITE));
        jtaEvent.setSize(140,30);
        return jtaEvent;
    }

    //拿到配置文件
    private void searchSlottingJsonFile(PsiClass psiClass){
        Collection<VirtualFile> virtualFiles =
                FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.allScope(project));
        for (VirtualFile virtualFile : virtualFiles) {
            slottingPsiFile = PsiManager.getInstance(project).findFile(virtualFile);
        }
    }

    /**
     * 弹窗时，检测是否已经有埋点，有的话展示出来
     */
    private void recoverPointData(){
        try {
            // 创建读取文件的字节流
            FileInputStream fis = new FileInputStream(slottingPsiFile.getVirtualFile().getPath());
            System.out.printf("Input file <<%s>> size is %dk\n",slottingPsiFile.getVirtualFile().getPath(), fis.available()/1024);
            byte[] fileBuf = new byte[fis.available()];
            fis.read(fileBuf);
            fis.close();


            JsonArray jsonArray = new JsonParser().parse(new String(fileBuf)).getAsJsonArray();
            ArrayList<EntryPointClassBean> pointClassBeans = new ArrayList<>();
            //循环遍历
            for (JsonElement user : jsonArray) {
                //通过反射 得到UserBean.class
                EntryPointClassBean userBean = gson.fromJson(user, new TypeToken<EntryPointClassBean>() {}.getType());
                pointClassBeans.add(userBean);
            }

            System.out.println("pointClassBeans.size() = "+pointClassBeans.size());

            EntryPointClassBean currentEntryPointClass = null;
            //找到当前选择的方法是否已经被添加了埋点
            for (EntryPointClassBean pointBean: pointClassBeans) {
                if(pointBean.classPath.equals(psiClass.getQualifiedName())){
                    //找到了当前类
                    currentEntryPointClass = pointBean;
                    break;
                }
            }

            if(null == currentEntryPointClass){
                return;
            }

            String currentSelText = editor.getSelectionModel().getSelectedText();
            //再判断是否已经为该方法添加了埋点
            for (EntryPointMethodBean methodBean: currentEntryPointClass.entryPoints) {
                if(methodBean.methodName.equals(currentSelText)){
                    //找到了当前类的该埋点方法
                    System.out.println("-----找到了埋点方法："+methodBean.methodName);
                    jtaSingleArea.setText(methodBean.event);
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGlobalIntellij(AnActionEvent event) {
        // 获取当前project对象
        project = event.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        psiFile = event.getData(CommonDataKeys.PSI_FILE);
        editor = event.getData(CommonDataKeys.EDITOR);
        // 获取Java类或者接口
        psiClass = getTargetClass(editor, psiFile);

        searchSlottingJsonFile(psiClass);
    }

    private void addInputSingleEvent() {
        JPanel jSingleEventPanel=new JPanel();
        JLabel jLabelSingleTip = new JLabel();
        jLabelSingleTip.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabelSingleTip.setText("Event：");

        jtaSingleArea = new JTextArea("",1,25);
        jtaSingleArea.setCaret(new MyTextAreaCaret());
        jtaSingleArea.setLineWrap(true);    //设置文本域中的文本为自动换行
        jtaSingleArea.setFont(new Font("楷体",Font.BOLD,16));    //修改字体样式
        jtaSingleArea.setBorder(BorderFactory.createMatteBorder(
                1, 1, 1, 1, JBColor.WHITE));
        jSingleEventPanel.add(jLabelSingleTip);
        jSingleEventPanel.add(jtaSingleArea);

        contentPanel.add(jSingleEventPanel);
    }

    private boolean addTopItem(AnActionEvent event) {
        // 获取全类名
        String qualifiedName = psiClass.getQualifiedName();
        String className = psiClass.getName();

        //获取当前选择的文本（即方法名）
        String currentSelText = editor.getSelectionModel().getSelectedText();

        PsiMethod[] psiMethods = psiClass.getAllMethods();

        //选中的是否是当前类的方法
        boolean isHaveCurMethod = false;
        for (int i = 0; i < psiMethods.length; i++) {
           PsiMethod psiMethod =  psiMethods[i];
           if(psiMethod.getName().equals(currentSelText)){
               isHaveCurMethod = true;
           }
        }
        if(!isHaveCurMethod){
            Messages.showMessageDialog("选择的方法名字不正确，请检查是否是属于该类的方法~","提示",Messages.getErrorIcon());
            return false;
        }

        JPanel cards=new JPanel();    //卡片式布局的面板

        //用来展示当前选中的class
        JPanel jCurClassPanel=new JPanel();
        jCurClassPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

//        jCurClassPanel.setBackground(JBColor.PINK);

        JLabel jLabel = new JLabel();
//        jLabel.setOpaque(true);
//        jLabel.setBackground(JBColor.PINK);
        jLabel.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabel.setText("当前class：");
        createJLabel(jCurClassPanel, jLabel,qualifiedName);

        //用来展示当前选中的方法名字
        JPanel jCurMethodPanel=new JPanel();
        jCurMethodPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel jLabelMethod = new JLabel();
        jLabelMethod.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabelMethod.setText("当前方法名字：");
        createJLabel(jCurMethodPanel, jLabelMethod,currentSelText);

        //用来选择是否插入在第一行
        JPanel jCurFirstLinePanel=new JPanel();
        jCurFirstLinePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel jLabelFirstLine = new JLabel();
        jLabelFirstLine.setFont(new java.awt.Font("Monotype Corsiva", 1, 16));
        jLabelFirstLine.setText("是否在第一行插入：");
        JCheckBox chkboxFirstLine =new JCheckBox("", true);    //创建指定文本和状态的复选框
        chkboxFirstLine.setFont(new Font("Monotype Corsiva", 1, 16));
        jCurFirstLinePanel.add(jLabelFirstLine);
        jCurFirstLinePanel.add(chkboxFirstLine);
        jCurFirstLinePanel.setPreferredSize(new Dimension(500,50));

        contentPanel.add(jCurClassPanel);
        contentPanel.add(jCurMethodPanel);
        contentPanel.add(jCurFirstLinePanel);
        contentPanel.add(cards);

        return true;
    }

    private void createJLabel(JPanel jCurClassPanel, JLabel jLabel,String showValue) {
        JLabel jLabelClassValue = new JLabel();
        jLabelClassValue.setFont(new Font("Monotype Corsiva", 1, 16));
        jLabelClassValue.setText(""+showValue);
        jCurClassPanel.add(jLabel);
        jCurClassPanel.add(jLabelClassValue);
        jCurClassPanel.setPreferredSize(new Dimension(500,30));
    }

    String _mChooseText;

    @Nullable
    protected PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        } else {
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ? null : target;
        }
    }

    private void setListData() {

        Project[] projects = ProjectManager.getInstance().getOpenProjects(); // 获取全部打开的项目
        ArrayList<String> _lstProjects = new ArrayList<String>();
        for (Project project:projects)
            _lstProjects.add(project.getBasePath());


        // 设置列表数据，并默认选择第一项 2017/3/18 09:50
        listView.setListData(_lstProjects.toArray());
        listView.setSelectedIndex(0);
        _mChooseText = _lstProjects.get(0);

        /**
         * 添加item点击事件 2017/3/18 09:38
         */
        listView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                _mChooseText = listView.getSelectedValue() + "";
            }
        });


        /**
         * 增加键盘处理事件 2017/3/18 09:35
         */
        listView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    onCancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onOK();
                }
            }
        });

        /**
         * 增加鼠标点击处理事件 2017/3/18 09:36
         */
        listView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    onOK();
                }
            }
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
//        SlottingDialog dialog = new SlottingDialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
    }
}
