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
package io.mattcarroll.hover.hoverdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.io.IOException;

import io.mattcarroll.hover.defaulthovermenu.view.ViewHoverMenu;

/**
 * Presents a Hover menu within an Activity (instead of presenting it on top of all other Windows).
 *
 * 在当前activity 打开悬浮窗
 *
 */
public class DemoHoverMenuActivity extends Activity {

    private static final String TAG = "HoverMenuActivity";

    private ViewHoverMenu mHoverMenuView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hover_menu);

        try {
            final ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.AppTheme);
            DemoHoverMenuAdapter adapter = new DemoHoverMenuFactory().createDemoMenuFromCode(contextThemeWrapper, Bus.getInstance());
//            DemoHoverMenuAdapter adapter = new DemoHoverMenuFactory().createDemoMenuFromFile(contextThemeWrapper);

            mHoverMenuView = (ViewHoverMenu) findViewById(R.id.hovermenu);
            mHoverMenuView.setAdapter(adapter);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create demo menu from file.");
            e.printStackTrace();
        }
    }

}
