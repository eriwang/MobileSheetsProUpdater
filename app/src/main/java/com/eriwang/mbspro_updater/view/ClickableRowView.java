package com.eriwang.mbspro_updater.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eriwang.mbspro_updater.R;

public class ClickableRowView extends LinearLayout
{
    public ClickableRowView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray attrsArray = context.obtainStyledAttributes(attrs, R.styleable.ClickableRowView);
        String text = attrsArray.getString(R.styleable.ClickableRowView_text);
        String subtext = attrsArray.getString(R.styleable.ClickableRowView_subtext);
        attrsArray.recycle();

        LayoutInflater inflater = (LayoutInflater) (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        inflater.inflate(R.layout.view_clickable_row, this, true);

        ((TextView) findViewById(R.id.clickable_row_text)).setText(text);

        TextView subtextView = findViewById(R.id.clickable_row_subtext);
        if (subtext == null)
        {
            subtextView.setHeight(0);
        }
        else
        {
            subtextView.setText(subtext);
        }
    }
}
