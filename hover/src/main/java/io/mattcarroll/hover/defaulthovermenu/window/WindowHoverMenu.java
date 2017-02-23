/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mattcarroll.hover.defaulthovermenu.window;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import io.mattcarroll.hover.HoverMenu;
import io.mattcarroll.hover.HoverMenuAdapter;
import io.mattcarroll.hover.Navigator;
import io.mattcarroll.hover.defaulthovermenu.HoverMenuView;

/**
 * {@link HoverMenu} implementation that displays within a {@code Window}.
 */
public class WindowHoverMenu implements HoverMenu {

    private static final String TAG = "WindowHoverMenu";

    private WindowViewController mWindowViewController; // Shows/hides/positions Views in a Window.
    private HoverMenuView mHoverMenuView; // The visual presentation of the Hover menu.
    private boolean mIsShowingHoverMenu; // Are we currently display mHoverMenuView?
    private boolean mIsInDragMode; // If we're not in drag mode then we're in menu mode.
    private Set<OnExitListener> mOnExitListeners = new HashSet<>();

    private HoverMenuView.HoverMenuTransitionListener mHoverMenuTransitionListener = new HoverMenuView.HoverMenuTransitionListener() {
        @Override
        public void onCollapsing() { }

        @Override
        public void onCollapsed() {
            mIsInDragMode = true;
            // When collapsed, we make mHoverMenuView untouchable so that the WindowDragWatcher can
            // take over. We do this so that touch events outside the drag area can propagate to
            // applications on screen.
            mWindowViewController.makeUntouchable(mHoverMenuView);
        }

        @Override
        public void onExpanding() {
            mIsInDragMode = false;
        }

        @Override
        public void onExpanded() {
            mWindowViewController.makeTouchable(mHoverMenuView);
        }
    };

    private HoverMenuView.HoverMenuExitRequestListener mMenuExitRequestListener = new HoverMenuView.HoverMenuExitRequestListener() {
        @Override
        public void onExitRequested() {
            hide();
        }
    };

    public WindowHoverMenu(@NonNull Context context, @NonNull WindowManager windowManager, @Nullable Navigator navigator, @Nullable String savedVisualState) {
        mWindowViewController = new WindowViewController(windowManager);

        InWindowDragger inWindowDragger = new InWindowDragger(
                context,
                mWindowViewController,
                ViewConfiguration.get(context).getScaledTouchSlop()
        );

        PointF anchorState = new PointF(2, 0.5f); // Default to right side, half way down. See CollapsedMenuAnchor.
        if (null != savedVisualState) {
            try {
                VisualStateMemento visualStateMemento = VisualStateMemento.fromJsonString(savedVisualState);
                anchorState.set(visualStateMemento.getAnchorSide(), visualStateMemento.getNormalizedPositionY());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mHoverMenuView = new HoverMenuView(context, navigator, inWindowDragger, anchorState);
        mHoverMenuView.setHoverMenuExitRequestListener(mMenuExitRequestListener);
        mHoverMenuView.setContentBackgroundColor(0xFF3b3b3b);
    }

    @Override
    public String getVisualState() {
        PointF anchor = mHoverMenuView.getAnchorState();
        return new VisualStateMemento((int) anchor.x, anchor.y).toJsonString();
    }

    @Override
    public void restoreVisualState(@NonNull String savedVisualState) {
        try {
            VisualStateMemento memento = VisualStateMemento.fromJsonString(savedVisualState);
            mHoverMenuView.setAnchorState(new PointF(memento.getAnchorSide(), memento.getNormalizedPositionY()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAdapter(@Nullable HoverMenuAdapter adapter) {
        mHoverMenuView.setAdapter(adapter);
    }

    /**
     * Initializes and displays the Hover menu. To destroy and remove the Hover menu, use {@link #hide()}.
     */
    @Override
    public void show() {
        if (!mIsShowingHoverMenu) {
            mWindowViewController.addView(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, false, mHoverMenuView);

            // Sync our control state with the HoverMenuView state.
            if (mHoverMenuView.isExpanded()) {
                mWindowViewController.makeTouchable(mHoverMenuView);
            } else {
                collapseMenu();
            }

            mIsShowingHoverMenu = true;
        }
    }

    /**
     * Exits the Hover menu system. This method is the inverse of {@link #show()}.
     */
    @Override
    public void hide() {
        if (mIsShowingHoverMenu) {
            mIsShowingHoverMenu = false;

            // Notify our exit listeners that we're exiting.
            notifyOnExitListeners();

            // Cleanup the control structures and Views.
            mWindowViewController.removeView(mHoverMenuView);
        }
    }

    /**
     * Expands the Hover menu to show all of its tabs and a content area for the selected tab. To
     * collapse the menu down a single active tab, use {@link #collapseMenu()}.
     */
    @Override
    public void expandMenu() {
        if (mIsInDragMode) {
            mHoverMenuView.expand();
        }
    }

    /**
     * Collapses the Hover menu down to its single active tab and allows the tab to be dragged
     * around the display. This method is the inverse of {@link #expandMenu()}.
     */
    @Override
    public void collapseMenu() {
        if (!mIsInDragMode) {
            mHoverMenuView.setHoverMenuTransitionListener(mHoverMenuTransitionListener);
            mHoverMenuView.collapse();
        }
    }

    @Override
    public void addOnExitListener(@NonNull OnExitListener onExitListener) {
        mOnExitListeners.add(onExitListener);
    }

    @Override
    public void removeOnExitListener(@NonNull OnExitListener onExitListener) {
        mOnExitListeners.remove(onExitListener);
    }

    private void notifyOnExitListeners() {
        for (OnExitListener listener : mOnExitListeners) {
            listener.onExitByUserRequest();
        }
    }

    private static class VisualStateMemento {

        private static final String JSON_KEY_ANCHOR_SIDE = "anchor_side";
        private static final String JSON_KEY_NORMALIZED_POSITION_Y = "normalized_position_y";

        public static VisualStateMemento fromJsonString(@NonNull String jsonString) throws JSONException {
            JSONObject jsonObject = new JSONObject(jsonString);
            int anchorSide = jsonObject.getInt(JSON_KEY_ANCHOR_SIDE);
            float normalizedPositionY = (float) jsonObject.getDouble(JSON_KEY_NORMALIZED_POSITION_Y);
            return new VisualStateMemento(anchorSide, normalizedPositionY);
        }

        private int mAnchorSide;
        private float mNormalizedPositionY;

        public VisualStateMemento(int anchorSide, float normalizedPositionY) {
            mAnchorSide = anchorSide;
            mNormalizedPositionY = normalizedPositionY;
        }

        public int getAnchorSide() {
            return mAnchorSide;
        }

        public float getNormalizedPositionY() {
            return mNormalizedPositionY;
        }

        public String toJsonString() {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_KEY_ANCHOR_SIDE, mAnchorSide);
                jsonObject.put(JSON_KEY_NORMALIZED_POSITION_Y, mNormalizedPositionY);
                return jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
