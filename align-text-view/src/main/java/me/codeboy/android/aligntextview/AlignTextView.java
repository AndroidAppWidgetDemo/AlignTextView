package me.codeboy.android.aligntextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 两端对齐的text view，可以设置最后一行靠左，靠右，居中对齐
 *
 * @author YD
 */
public class AlignTextView extends TextView {

    private Align align = Align.ALIGN_LEFT; // 默认最后一行左对齐


    /**
     *
     */
    // 初始化计算
    private boolean firstCalc = true;


    /**
     *
     */
    // textView宽度
    private int width;
    // 原始高度
    private int originalHeight = 0;
    // 原始行数
    private int originalLineCount = 0;
    // 原始paddingBottom
    private int originalPaddingBottom = 0;
    // 单行文字高度
    private float textHeight;
    // 行间距
    private float lineSpacingAdd = 0.0f;
    // 行间距的倍数
    private float lineSpacingMultiplier = 1.0f;

    /**
     *
     */
    // 分割后的行
    private List<String> lines = new ArrayList<String>();
    // 尾行 index
    private List<Integer> tailLines = new ArrayList<Integer>();
    // 行间距
    private float textLineSpaceExtra = 0;
    // textview的高度 是AlignTextView更改后的
    private boolean setPaddingFromMe = false;


    // 尾行对齐方式
    public enum Align {
        ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT  // 居中，居左，居右,针对段落最后一行
    }

    public AlignTextView(Context context) {
        super(context);
        // 不能选择复制
        setTextIsSelectable(false);
    }

    public AlignTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 不能选择复制
        setTextIsSelectable(false);
        //
        int[] attributes = new int[]{android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier};
        TypedArray arr = context.obtainStyledAttributes(attrs, attributes);
        // 行间距
        lineSpacingAdd = arr.getDimensionPixelSize(0, 0);
        // 行间距的倍数
        lineSpacingMultiplier = arr.getFloat(1, 1.0f);
        // 原始paddingBottom
        originalPaddingBottom = getPaddingBottom();
        arr.recycle();

        // 尾行对齐方式
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AlignTextView);
        int alignStyle = ta.getInt(R.styleable.AlignTextView_align, 0);
        switch (alignStyle) {
            case 1:
                align = Align.ALIGN_CENTER;
                break;
            case 2:
                align = Align.ALIGN_RIGHT;
                break;
            default:
                align = Align.ALIGN_LEFT;
                break;
        }

        ta.recycle();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        //首先进行高度调整
        if (firstCalc) {
            // 获取宽度
            width = getMeasuredWidth();
            // 获取text
            String text = getText().toString();
            // 画笔
            TextPaint paint = getPaint();

            // ---分割行---
            // 分割后的行
            lines.clear();
            tailLines.clear();

            // 文本含有换行符时，分割单独处理
            String[] items = text.split("\\n");
            for (String item : items) {
                calc(paint, item);
            }

            // ---计算原始宽高等数据---
            //使用替代textview计算原始高度与行数
            measureTextViewHeight(text, paint.getTextSize(), getMeasuredWidth() -
                    getPaddingLeft() - getPaddingRight());
            // 获取行高
            textHeight = 1.0f * originalHeight / originalLineCount;

            // ----------------
            // 行间距
            textLineSpaceExtra = textHeight * (lineSpacingMultiplier - 1) + lineSpacingAdd;

            // 多出来多少行的距离
            //计算实际高度,加上多出的行的高度(一般是减少)
            int heightGap = (int) ((textLineSpaceExtra + textHeight) * (lines.size() -
                    originalLineCount));

            // 自己设置padding
            setPaddingFromMe = true;
            //调整textview的paddingBottom来缩小底部空白
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                    originalPaddingBottom + heightGap);

            firstCalc = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 画笔
        TextPaint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        paint.drawableState = getDrawableState();
        // 宽
        width = getMeasuredWidth();

        // ？？？？
        Paint.FontMetrics fm = paint.getFontMetrics();
        float firstHeight = getTextSize() - (fm.bottom - fm.descent + fm.ascent - fm.top);
        //
        int gravity = getGravity();
        if ((gravity & 0x1000) == 0) { // 是否垂直居中
            firstHeight = firstHeight + (textHeight - firstHeight) / 2;
        }
        // 计算宽度
        int paddingTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        width = width - paddingLeft - paddingRight;
        // 行
        for (int i = 0; i < lines.size(); i++) {
            // 当前行的Y坐标
            float drawY = i * textHeight + firstHeight;
            // 获取当前行的文字
            String line = lines.get(i);
            // 绘画起始x坐标
            float drawSpacingX = paddingLeft;
            // 空出的距离
            float gap = (width - paint.measureText(line));
            // 间隔
            float interval = gap / (line.length() - 1);

            // 绘制最后一行
            if (tailLines.contains(i)) {
                interval = 0;
                if (align == Align.ALIGN_CENTER) {
                    drawSpacingX += gap / 2;
                } else if (align == Align.ALIGN_RIGHT) {
                    drawSpacingX += gap;
                }
            }
            // 非最后一行 一个字 一个字的绘制
            for (int j = 0; j < line.length(); j++) {
                float drawX = paint.measureText(line.substring(0, j)) + interval * j;
                canvas.drawText(line.substring(j, j + 1), drawX + drawSpacingX, drawY +
                        paddingTop + textLineSpaceExtra * i, paint);
            }
        }
    }

    /**
     * 设置尾行对齐方式
     *
     * @param align 对齐方式
     */
    public void setAlign(Align align) {
        this.align = align;
        invalidate();
    }

    /**
     * 计算每行应显示的文本数
     *
     * @param paint 画笔
     * @param text  文本
     */
    private void calc(Paint paint, String text) {
        // 文本长度为0
        if (text.length() == 0) {
            // 换行
            lines.add("\n");
            return;
        }
        // 起始位置
        int startPosition = 0;
        // 一个中文的长度
        float oneChineseWidth = paint.measureText("中");
        // 一行可以显示多少个中文 (空格该怎么办？？？)
        int ignoreCalcLength = (int) (width / oneChineseWidth);
        // 取一行可显示长度 (空格该怎么办？？？)
        StringBuilder sb = new StringBuilder(text.substring(0, Math.min(ignoreCalcLength + 1,
                text.length())));
        //
        for (int i = ignoreCalcLength + 1; i < text.length(); i++) {
            // 已满一行的数据了
            if (paint.measureText(text.substring(startPosition, i + 1)) > width) {
                // index 赋值
                startPosition = i;
                //将之前的字符串加入列表中
                lines.add(sb.toString());

                //添加开始忽略的字符串，长度不足的话直接结束,否则继续
                sb = new StringBuilder();
                if ((text.length() - startPosition) > ignoreCalcLength) {
                    sb.append(text.substring(startPosition, startPosition + ignoreCalcLength));
                } else {
                    lines.add(text.substring(startPosition));
                    break;
                }
                //
                i = i + ignoreCalcLength - 1;
            } else {
                sb.append(text.charAt(i));
            }
        }
        // 一行 不足ignoreCalcLength的长度
        if (sb.length() > 0) {
            lines.add(sb.toString());
        }
        // 尾行 index
        tailLines.add(lines.size() - 1);
    }


    @Override
    public void setText(CharSequence text, BufferType type) {
        firstCalc = true;
        super.setText(text, type);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (!setPaddingFromMe) {
            originalPaddingBottom = bottom;
        }
        setPaddingFromMe = false;
        super.setPadding(left, top, right, bottom);
    }


    /**
     * 获取文本实际所占高度，辅助用于计算行高,行数
     *
     * @param text        文本
     * @param textSize    字体大小
     * @param deviceWidth 屏幕宽度
     */
    private void measureTextViewHeight(String text, float textSize, int deviceWidth) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(deviceWidth, MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        originalLineCount = textView.getLineCount();
        originalHeight = textView.getMeasuredHeight();
    }
}