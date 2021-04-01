package com.github.mangaloid.client.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadingBar extends View {

    private int chunksCount = -1;
    private List<Float> chunkLoadingProgress = new ArrayList<>();
    private Paint paint;

    public LoadingBar(Context context) {
        super(context);
        init();
    }

    public LoadingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
    }

    public void setProgress(Float updatedProgress) {
        setProgress(Collections.singletonList(updatedProgress));
    }

    public void setProgress(List<Float> updatedProgress) {
        // This branch should only happen once for each download so it should be fine to re-allocate
        // the list here
        if (chunksCount == -1 || chunksCount != updatedProgress.size()) {
            chunksCount = updatedProgress.size();

            chunkLoadingProgress.clear();
            chunkLoadingProgress.addAll(updatedProgress);
        }

        for (int i = 0; i < updatedProgress.size(); i++) {
            float updatedChunkProgress = updatedProgress.get(i);
            float clampedProgress = Math.min(Math.max(updatedChunkProgress, .1f), 1f);

            chunkLoadingProgress.set(i, clampedProgress);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = (float) getWidth() / chunksCount;
        float offset = 0f;

        for (int i = 0; i < chunkLoadingProgress.size(); i++) {
            float progress = chunkLoadingProgress.get(i);
            if (progress > 0f) {
                canvas.drawRect(offset, 0f, offset + (width * progress), getHeight(), paint);
            }

            offset += width;
        }
    }

}
