/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Intent;

import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.ui.TaplTestsLauncher3;
import com.android.quickstep.TaskbarModeSwitchRule.TaskbarModeSwitch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TaplTestsSplitscreen extends AbstractQuickStepTest {
    private static final String CALCULATOR_APP_NAME = "Calculator";
    private static final String CALCULATOR_APP_PACKAGE =
            resolveSystemApp(Intent.CATEGORY_APP_CALCULATOR);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        TaplTestsLauncher3.initialize(this);

        if (mLauncher.isTablet()) {
            mLauncher.enableBlockTimeout(true);
            mLauncher.showTaskbarIfHidden();
        }
    }

    @After
    public void tearDown() {
        if (mLauncher.isTablet()) {
            mLauncher.enableBlockTimeout(false);
        }
    }

    @Test
    @PortraitLandscape
    public void testSplitFromOverview() {
        createAndLaunchASplitPair();
    }

    @Test
    @PortraitLandscape
    @TaskbarModeSwitch
    public void testSplitAppFromHomeWithItself() throws Exception {
        // Currently only tablets have Taskbar in Overview, so test is only active on tablets
        assumeTrue(mLauncher.isTablet());

        mLauncher.getWorkspace()
                .deleteAppIcon(mLauncher.getWorkspace().getHotseatAppIcon(0))
                .switchToAllApps()
                .getAppIcon(CALCULATOR_APP_NAME)
                .dragToHotseat(0);

        startAppFast(CALCULATOR_APP_PACKAGE);

        mLauncher.goHome()
                .switchToAllApps()
                .getAppIcon(CALCULATOR_APP_NAME)
                .openMenu()
                .getSplitScreenMenuItem()
                .click();

        mLauncher.getLaunchedAppState()
                .getTaskbar()
                .getAppIcon(CALCULATOR_APP_NAME)
                .launchIntoSplitScreen();
    }

    @Test
    public void testSaveAppPairMenuItemExistsOnSplitPair() throws Exception {
        assumeTrue(FeatureFlags.ENABLE_APP_PAIRS.get());

        createAndLaunchASplitPair();

        assertTrue("Save app pair menu item is missing",
                mLauncher.goHome()
                        .switchToOverview()
                        .getCurrentTask()
                        .tapMenu()
                        .hasMenuItem("Save app pair"));
    }

    @Test
    public void testSaveAppPairMenuItemDoesNotExistOnSingleTask() throws Exception {
        assumeTrue(FeatureFlags.ENABLE_APP_PAIRS.get());

        startAppFast(CALCULATOR_APP_PACKAGE);

        assertFalse("Save app pair menu item is erroneously appearing on single task",
                mLauncher.goHome()
                        .switchToOverview()
                        .getCurrentTask()
                        .tapMenu()
                        .hasMenuItem("Save app pair"));
    }

    private void createAndLaunchASplitPair() {
        startTestActivity(2);
        startTestActivity(3);

        if (mLauncher.isTablet()) {
            mLauncher.goHome().switchToOverview().getOverviewActions()
                    .clickSplit()
                    .getTestActivityTask(2)
                    .open();
        } else {
            mLauncher.goHome().switchToOverview().getCurrentTask()
                    .tapMenu()
                    .tapSplitMenuItem()
                    .getCurrentTask()
                    .open();
        }
    }
}
