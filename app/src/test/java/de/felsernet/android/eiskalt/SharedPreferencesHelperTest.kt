package de.felsernet.android.eiskalt

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.content.SharedPreferences

class SharedPreferencesHelperTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        // Use reflection to set the private field
        val field = SharedPreferencesHelper::class.java.getDeclaredField("sharedPreferences")
        field.isAccessible = true
        field.set(SharedPreferencesHelper, mockSharedPreferences)
    }

    @Test
    fun saveLastViewedList() {
        SharedPreferencesHelper.saveLastViewedList("testList")
        verify(mockEditor).putString("last_viewed_list", "testList")
        verify(mockEditor).apply()
    }

    @Test
    fun getLastViewedList() {
        `when`(mockSharedPreferences.getString("last_viewed_list", null)).thenReturn("testList")
        val result = SharedPreferencesHelper.getLastViewedList()
        assert(result == "testList")
        verify(mockSharedPreferences).getString("last_viewed_list", null)
    }

    @Test
    fun getLastViewedList_returnsNull() {
        `when`(mockSharedPreferences.getString("last_viewed_list", null)).thenReturn(null)
        val result = SharedPreferencesHelper.getLastViewedList()
        assert(result == null)
        verify(mockSharedPreferences).getString("last_viewed_list", null)
    }

    @Test
    fun clearLastViewedList() {
        SharedPreferencesHelper.clearLastViewedList()
        verify(mockEditor).remove("last_viewed_list")
        verify(mockEditor).apply()
    }

    @Test
    fun saveCustomTitle() {
        SharedPreferencesHelper.saveCustomTitle("testTitle")
        verify(mockEditor).putString("custom_title", "testTitle")
        verify(mockEditor).apply()
    }

    @Test
    fun getCustomTitle() {
        `when`(mockSharedPreferences.getString("custom_title", null)).thenReturn("testTitle")
        val result = SharedPreferencesHelper.getCustomTitle()
        assert(result == "testTitle")
        verify(mockSharedPreferences).getString("custom_title", null)
    }

    @Test
    fun getCustomTitle_returnsNull() {
        `when`(mockSharedPreferences.getString("custom_title", null)).thenReturn(null)
        val result = SharedPreferencesHelper.getCustomTitle()
        assert(result == null)
        verify(mockSharedPreferences).getString("custom_title", null)
    }

    @Test
    fun clearCustomTitle() {
        SharedPreferencesHelper.clearCustomTitle()
        verify(mockEditor).remove("custom_title")
        verify(mockEditor).apply()
    }
}
