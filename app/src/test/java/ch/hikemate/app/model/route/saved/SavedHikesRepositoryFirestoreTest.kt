package ch.hikemate.app.model.route.saved

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldValue.arrayRemove
import com.google.firebase.firestore.FieldValue.arrayUnion
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.not
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedHikesRepositoryFirestoreTest {
  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockFirebaseAuth: FirebaseAuth
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockDocumentSnapshot: DocumentSnapshot

  private lateinit var savedHikesRepositoryFirestore: SavedHikesRepositoryFirestore

  private val userUid = "testUserUid"
  private val savedHike = SavedHike("3", "Hike 3", null)
  private val plannedHike = SavedHike("5", "Hike 5", Timestamp(0, 0))
  private val userSavedHikes =
      SavedHikesRepositoryFirestore.UserSavedHikes(listOf(savedHike, plannedHike))

  @Before
  fun setUp() {
    openMocks(this)

    savedHikesRepositoryFirestore = SavedHikesRepositoryFirestore(mockFirestore, mockFirebaseAuth)

    // If the collection is the one from the user, return the mock collection reference
    `when`(mockFirestore.collection(eq(SavedHikesRepositoryFirestore.SAVED_HIKES_COLLECTION)))
        .thenReturn(mockCollectionReference)
    // If the collection is not the one from the user, throw an exception
    `when`(mockFirestore.collection(not(eq(SavedHikesRepositoryFirestore.SAVED_HIKES_COLLECTION))))
        .then { fail("Unexpected collection reference: ${it.arguments[0]}") }

    `when`(mockCollectionReference.document(eq(userUid))).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document(not(eq(userUid)))).then {
      fail("Unexpected document reference: ${it.arguments[0]}")
    }
    `when`(mockDocumentReference.update(any<String>(), any())).thenReturn(Tasks.forResult(null))
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))
    `when`(mockDocumentSnapshot.toObject(SavedHikesRepositoryFirestore.UserSavedHikes::class.java))
        .thenReturn(userSavedHikes)

    `when`(mockDocumentSnapshot.exists()).thenReturn(true)
    signInUser()
  }

  private fun signInUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(mock())
    `when`(mockFirebaseAuth.currentUser?.uid).thenReturn(userUid)
  }

  private fun signOutUser() {
    `when`(mockFirebaseAuth.currentUser).thenReturn(null)
  }

  @Test
  fun repositoryDoesntWorkWithNoUser() =
      runTest(timeout = 5.seconds) {
        // Given
        signOutUser()

        // When
        try {
          savedHikesRepositoryFirestore.loadSavedHikes()
          fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {}

        try {
          savedHikesRepositoryFirestore.addSavedHike(savedHike)
          fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {}

        try {
          savedHikesRepositoryFirestore.removeSavedHike(savedHike)
          fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {}

        try {
          savedHikesRepositoryFirestore.getSavedHike(savedHike.id)
          fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {}

        try {
          savedHikesRepositoryFirestore.isHikeSaved(savedHike.id)
          fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {}
      }

  @Test
  fun loadHikesWorksCorrectly() =
      runTest(timeout = 5.seconds) {
        // Given
        val expected = listOf(savedHike, plannedHike)

        // When
        val result = savedHikesRepositoryFirestore.loadSavedHikes()

        // Then
        assertEquals(expected, result)
      }

  @Test
  fun removeSavedHikeUnSavesTheHike() =
      runTest(timeout = 5.seconds) {
        // When
        savedHikesRepositoryFirestore.removeSavedHike(savedHike)

        // Then
        val arrayRemoveArgument: ArgumentCaptor<FieldValue> =
            ArgumentCaptor.forClass(FieldValue::class.java)
        verify(mockDocumentReference)
            .update(
                eq(SavedHikesRepositoryFirestore.UserSavedHikes::savedHikes.name),
                arrayRemoveArgument.capture())
        // We need to check the class of the argument, because the values are private and cannot be
        // compared
        assertEquals(arrayRemove(savedHike).javaClass, arrayRemoveArgument.value.javaClass)
      }

  @Test
  fun addSavedHikeSavesTheHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val newHike = SavedHike("new", "New Hike", null)

        // When
        savedHikesRepositoryFirestore.addSavedHike(newHike)

        // Then
        val arrayUnionArgument: ArgumentCaptor<FieldValue> =
            ArgumentCaptor.forClass(FieldValue::class.java)
        verify(mockDocumentReference)
            .update(
                eq(SavedHikesRepositoryFirestore.UserSavedHikes::savedHikes.name),
                arrayUnionArgument.capture())
        // We need to check the class of the argument, because the values are private and cannot be
        // compared
        assertEquals(arrayUnion(newHike).javaClass, arrayUnionArgument.value.javaClass)
      }

  @Test
  fun addSavedHikeWithNoSavedHikesCreatesNewDocument() =
      runTest(timeout = 5.seconds) {
        // Given
        val newHike = SavedHike("new", "New Hike", null)

        `when`(mockDocumentSnapshot.exists()).thenReturn(false)

        // When
        savedHikesRepositoryFirestore.addSavedHike(newHike)

        // Then
        verify(mockDocumentReference)
            .set(SavedHikesRepositoryFirestore.UserSavedHikes(listOf(newHike)))
      }

  @Test
  fun getSavedHikeReturnsCorrectHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val expected = savedHike

        // When
        val result = savedHikesRepositoryFirestore.getSavedHike(savedHike.id)

        // Then
        assertEquals(expected, result)
      }

  @Test
  fun isHikeSavedReturnsTrueForSavedHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val id = savedHike.id

        // When
        val result = savedHikesRepositoryFirestore.isHikeSaved(id)

        // Then
        assertEquals(true, result)
      }

  @Test
  fun isHikeSavedReturnsFalseForUnsavedHike() =
      runTest(timeout = 5.seconds) {
        // Given
        val id = "notSaved"

        // When
        val result = savedHikesRepositoryFirestore.isHikeSaved(id)

        // Then
        assertEquals(false, result)
      }

  @Test
  fun removingNonExistentHikeChangeNothing() =
      runTest(timeout = 5.seconds) {
        // Given
        val nonExistentHike = SavedHike("nonExistent", "Non Existent Hike", null)

        // When
        savedHikesRepositoryFirestore.removeSavedHike(nonExistentHike)

        // Then
        val result = savedHikesRepositoryFirestore.loadSavedHikes()
        assertEquals(listOf(savedHike, plannedHike), result)
      }

  @Test
  fun addingAlreadySavedHikeChangeNothing() =
      runTest(timeout = 5.seconds) {
        // Given
        val alreadySavedHike = savedHike

        // When
        savedHikesRepositoryFirestore.addSavedHike(alreadySavedHike)

        // Then
        val result = savedHikesRepositoryFirestore.loadSavedHikes()
        assertEquals(listOf(savedHike, plannedHike), result)
      }
}
