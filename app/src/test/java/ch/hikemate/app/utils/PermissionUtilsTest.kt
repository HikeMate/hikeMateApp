package ch.hikemate.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PermissionUtilsTest {
  @Test
  fun firstTimeAskingPermissionGetsFromSharedPreferences() {
    // Given
    val permissionName = "location"
    val sharedPreferences = mockk<SharedPreferences>()
    every { sharedPreferences.getBoolean(eq(permissionName), eq(true)) } returns false
    val context = mockk<Context>()
    every { context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)) } returns
        sharedPreferences

    // When
    val isFirstTime = PermissionUtils.firstTimeAskingPermission(context, permissionName)

    // Then
    verify { context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE)) }
    verify { sharedPreferences.getBoolean(permissionName, true) }
    assert(!isFirstTime)
  }

  @OptIn(ExperimentalPermissionsApi::class)
  @Test
  fun anyPermissionGrantedChecksRevokedPermissions() {
    // Given
    val state = mockk<MultiplePermissionsState>()
    every { state.revokedPermissions } returns listOf()
    every { state.permissions } returns listOf()

    // When
    val anyGranted = PermissionUtils.anyPermissionGranted(state)

    // Then
    verify { state.revokedPermissions }
    verify { state.permissions }
    assert(!anyGranted)
  }
}
