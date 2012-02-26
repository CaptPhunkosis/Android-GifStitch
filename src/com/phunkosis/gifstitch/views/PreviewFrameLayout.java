/*
 *  GifStitch - Android App
 *  Copyright (C) 2012 Nick Thuesen
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.phunkosis.gifstitch.views;

import com.phunkosis.gifstitch.R;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A layout which handles the preview aspect ratio and the position of
 * the gripper.  (Like in the default camera app.)
 */
public class PreviewFrameLayout extends ViewGroup {
    private static final int MIN_HORIZONTAL_MARGIN = 10; // 10dp

    /** A callback to be invoked when the preview frame's size changes. */
    public interface OnSizeChangedListener {
        public void onSizeChanged();
    }

    //private double mAspectRatio = 4.0 / 3.0;
    private double mAspectRatio = -1.0;
    private FrameLayout mFrame;
    private OnSizeChangedListener mSizeListener;
    private final DisplayMetrics mMetrics = new DisplayMetrics();

    public PreviewFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        ((Activity) context).getWindowManager()
                .getDefaultDisplay().getMetrics(mMetrics);
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mSizeListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        mFrame = (FrameLayout) findViewById(R.id.frame);
        if (mFrame == null) {
            throw new IllegalStateException(
                    "must provide child with id as \"frame\"");
        }
    }

    public void setAspectRatio(double ratio) {
        //if (ratio <= 0.0) throw new IllegalArgumentException();
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Try to layout the "frame" in the center of the area, and put
        // "gripper" just to the left of it. If there is no enough space for
        // the gripper, the "frame" will be moved a little right so that
        // they won't overlap with each other.

        int frameWidth = getWidth();
        int frameHeight = getHeight();

        FrameLayout f = mFrame;

        int horizontalPadding = Math.max(
                f.getPaddingLeft() + f.getPaddingRight(),
                (int) (MIN_HORIZONTAL_MARGIN * mMetrics.density));
        int verticalPadding = f.getPaddingBottom() + f.getPaddingTop();

        // Ignore the vertical paddings, so that we won't draw the frame on the
        // top and bottom sides
        int previewHeight = frameHeight;
        int previewWidth = frameWidth - horizontalPadding;

        // resize frame and preview for aspect ratio
        if(mAspectRatio >= 0.0){
	        if (previewWidth > previewHeight * mAspectRatio) {
	            previewWidth = (int) (previewHeight * mAspectRatio + .5);
	        } else {
	            previewHeight = (int) (previewWidth / mAspectRatio + .5);
	        }
        }

        frameWidth = previewWidth + horizontalPadding;
        frameHeight = previewHeight + verticalPadding;

        int hSpace = ((r - l) - frameWidth) / 2;
        int vSpace = ((b - t) - frameHeight) / 2;
        mFrame.measure(
                MeasureSpec.makeMeasureSpec(frameWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(frameHeight, MeasureSpec.EXACTLY));
        mFrame.layout(l + hSpace, t + vSpace, r - hSpace, b - vSpace);
        if (mSizeListener != null) {
            mSizeListener.onSizeChanged();
        }
    }
}