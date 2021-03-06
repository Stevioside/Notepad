package xiaoguang.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义控件，用于图文并排
 */
public class SpanText extends EditText {

    public SpanText(Context context) {
        super(context);
    }

    public SpanText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 让图片路径增加到日记字符串里，并且重新定位光标
     *
     * @param imgPath
     */
    public void addImgToText(String imgPath) {
        if (getText().length() == 0) {
            this.setText(" ");
        }
        SpannableString ss = new SpannableString(imgPath);
        this.append(ss);
        setPicText(this.getText().toString());
        this.setSelection(this.getText().toString().length());  //光标位置
    }

    /**
     * 解析字符串文本，普通文字正常显示，路径显示为图片
     *
     * @param content
     */
    public void setPicText(String content) {
        SpannableString s1 = new SpannableString(content);
        Pattern pattern = Pattern.compile(Constants.PicPatten);
        Matcher mc = pattern.matcher(content);//正则查找图片路径
        while (mc.find()) {
            Bitmap bitmap;
            BitMapUtil.Size size = BitMapUtil.getBitMapSize(mc.group());
            bitmap = BitMapUtil.getBitmap(mc.group(), size.getWidth() / 2, size.getHeight() / 2);
            ImageSpan imgSpan = new ImageSpan(bitmap, ImageSpan.ALIGN_BASELINE);
            s1.setSpan(imgSpan, mc.start(), mc.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        this.setText(s1);
    }
}



