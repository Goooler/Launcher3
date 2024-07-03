/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3.folder

import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.launcher3.DropTarget.DragObject
import com.android.launcher3.LauncherAppState
import com.android.launcher3.celllayout.board.FolderPoint
import com.android.launcher3.celllayout.board.TestWorkspaceBuilder
import com.android.launcher3.util.ActivityContextWrapper
import com.android.launcher3.util.ModelTestExtensions.clearModelDb
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/** Tests for [Folder] */
@SmallTest
@RunWith(AndroidJUnit4::class)
class FolderTest {

    private val context: Context =
        ActivityContextWrapper(ApplicationProvider.getApplicationContext())
    private val workspaceBuilder = TestWorkspaceBuilder(context)
    private val folder: Folder = Mockito.spy(Folder(context, null))

    @After
    fun tearDown() {
        LauncherAppState.getInstance(context).model.clearModelDb()
    }

    @Test
    fun `Undo a folder with 1 icon when onDropCompleted is called`() {
        val folderInfo =
            workspaceBuilder.createFolderInCell(FolderPoint(Point(1, 0), TWO_ICON_FOLDER_TYPE), 0)
        folder.mInfo = folderInfo
        folder.mInfo.getContents().removeAt(0)
        folder.mContent = Mockito.mock(FolderPagedView::class.java)
        val dragLayout = Mockito.mock(View::class.java)
        val dragObject = Mockito.mock(DragObject::class.java)
        assertEquals(folder.deleteFolderOnDropCompleted, false)
        folder.onDropCompleted(dragLayout, dragObject, true)
        verify(folder, times(1)).replaceFolderWithFinalItem()
        assertEquals(folder.deleteFolderOnDropCompleted, false)
    }

    @Test
    fun `Do not undo a folder with 2 icons when onDropCompleted is called`() {
        val folderInfo =
            workspaceBuilder.createFolderInCell(FolderPoint(Point(1, 0), TWO_ICON_FOLDER_TYPE), 0)
        folder.mInfo = folderInfo
        folder.mContent = Mockito.mock(FolderPagedView::class.java)
        val dragLayout = Mockito.mock(View::class.java)
        val dragObject = Mockito.mock(DragObject::class.java)
        assertEquals(folder.deleteFolderOnDropCompleted, false)
        folder.onDropCompleted(dragLayout, dragObject, true)
        verify(folder, times(0)).replaceFolderWithFinalItem()
        assertEquals(folder.deleteFolderOnDropCompleted, false)
    }

    companion object {
        const val TWO_ICON_FOLDER_TYPE = 'A'
    }
}
