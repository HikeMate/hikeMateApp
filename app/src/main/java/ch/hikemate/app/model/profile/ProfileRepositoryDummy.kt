package ch.hikemate.app.model.profile

import ch.hikemate.app.ui.profile.ProfileScreen

/** A dummy implementation of the profile repository. */
class ProfileRepositoryDummy : ProfileRepository {

  override fun getNewUid(): String {
    return "1"
  }

  override fun init(onSuccess: () -> Unit) {
    onSuccess()
  }

  override fun getProfileById(
      id: String,
      onSuccess: (Profile) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    onSuccess(ProfileScreen.DEFAULT_PROFILE)
  }

  override fun addProfile(profile: Profile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    onSuccess()
  }

  override fun updateProfile(
      profile: Profile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    onSuccess()
  }

  override fun deleteProfileById(
      id: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    onSuccess()
  }
}
