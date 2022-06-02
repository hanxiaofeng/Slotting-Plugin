package com.slotting.marker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.PsiClassImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class HaloLineMarker implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        LineMarkerInfo lineMarkerInfo= null;
        try {
            System.out.println(psiElement);
            System.out.println(psiElement);


//            lineMarkerInfo = null;
//            String anno="org.springframework.boot.autoconfigure.SpringBootApplication";
//            if(!judgeHaveAnnotation(psiElement,anno)){
//                return lineMarkerInfo;
//            }
//            PsiClassImpl field = ((PsiClassImpl) psiElement);
//            PsiAnnotation psiAnnotation = field.getAnnotation(anno);
//            lineMarkerInfo = new LineMarkerInfo<>(psiAnnotation, psiAnnotation.getTextRange(), IconLoader.findIcon("/icons/right/HaloBasic.png"),
//                    new FunctionTooltip("快速导航"),
//                    new AppMgmtNavigationHandler(),// ➊
//                    GutterIconRenderer.Alignment.LEFT);
        } catch (Exception e) {
            e.printStackTrace(); // ➋
        }
        return lineMarkerInfo;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> list, @NotNull Collection<? super LineMarkerInfo<?>> collection) {
    }

    private boolean judgeHaveAnnotation(@NotNull PsiElement psiElement, String anno) {
        if (psiElement instanceof PsiClass) {
            PsiClassImpl field = ((PsiClassImpl) psiElement);
            PsiAnnotation psiAnnotation = field.getAnnotation(anno);
            if (null != psiAnnotation) {
                return true;
            }
            return false;
        }
        return false;
    }

}
