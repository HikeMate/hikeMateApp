package ch.hikemate.app.model.profile

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.fail
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify

@Suppress("UNCHECKED_CAST")
class ProfileRepositoryFirestoreTest {
  @Mock private lateinit var mockFirestore: FirebaseFirestore
  @Mock private lateinit var mockDocumentReference: DocumentReference
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockDocumentSnapshot: DocumentSnapshot
  @Mock private lateinit var mockProfileQuery: QuerySnapshot
  @Mock private lateinit var firebaseAuth: com.google.firebase.auth.FirebaseAuth

  private lateinit var repository: ProfileRepositoryFirestore

  private val profile =
      Profile(
          id = "1",
          name = "John Doe",
          email = "john.doe@gmail.com",
          hikingLevel = HikingLevel.INTERMEDIATE,
          joinedDate = Timestamp(1609459200, 0))

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    repository = ProfileRepositoryFirestore(mockFirestore)
    firebaseAuth = mock()

    `when`(mockFirestore.collection(any())).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.document(any())).thenReturn(mockDocumentReference)
    `when`(mockCollectionReference.document()).thenReturn(mockDocumentReference)
  }

  @Test
  fun getNewUid() {
    `when`(mockDocumentReference.id).thenReturn("1")
    val uid = repository.getNewUid()
    assert(uid == "1")
  }

  @Test
  fun createProfileWorks() {
    val profileMock = mock(FirebaseUser::class.java)
    `when`(firebaseAuth.currentUser).thenReturn(profileMock)
    `when`(firebaseAuth.currentUser!!.uid).thenReturn("1")
    `when`(firebaseAuth.currentUser!!.displayName).thenReturn("John Doe")
    `when`(firebaseAuth.currentUser!!.email).thenReturn("john.doe@gmail.com")

    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null)) // Simulate success
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(mockDocumentSnapshot)
    `when`(mockDocumentReference.get()).thenReturn(task)

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())
    repository.createProfile(
        firebaseAuth.currentUser,
        onSuccess = { profile_ ->
          assert(profile_.id == "1")
          assert(profile_.name == "John Doe")
          assert(profile_.email == "john.doe@gmail.com")
          assert(profile_.hikingLevel == HikingLevel.BEGINNER)
        },
        onFailure = { fail("Failure callback should not be called") })

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun documentToProfile() {
    val testTimestamp = Timestamp(1609459200, 0)
    `when`(mockDocumentSnapshot.id).thenReturn("1")
    `when`(mockDocumentSnapshot.getString("name")).thenReturn("John Doe")
    `when`(mockDocumentSnapshot.getString("email")).thenReturn("john.doe@gmail.com")
    `when`(mockDocumentSnapshot.getString("hikingLevel")).thenReturn("INTERMEDIATE")
    `when`(mockDocumentSnapshot.getTimestamp("joinedDate")).thenReturn(testTimestamp)

    val profile = repository.documentToProfile(mockDocumentSnapshot)
    assert(profile != null)
    assert(profile!!.id == "1")
    assert(profile.name == "John Doe")
    assert(profile.email == "john.doe@gmail.com")
    assert(profile.hikingLevel == HikingLevel.INTERMEDIATE)
    assert(profile.joinedDate == testTimestamp)
  }

  @Test
  fun documentToProfile_returnsNullIfDataIsMissing() {
    `when`(mockDocumentSnapshot.id).thenReturn("1")
    `when`(mockDocumentSnapshot.getString("name")).thenReturn("John Doe")
    `when`(mockDocumentSnapshot.getString("email")).thenThrow(RuntimeException("No email field"))
    `when`(mockDocumentSnapshot.getString("hikingLevel")).thenReturn("INTERMEDIATE")
    `when`(mockDocumentSnapshot.getTimestamp("joinedDate")).thenReturn(Timestamp(1609459200, 0))

    val profile = repository.documentToProfile(mockDocumentSnapshot)

    assert(profile == null)
  }

  @Test
  fun getProfile_callsDocuments() {
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))

    // Call the method under test
    repository.getProfileById(
        "whatever",
        onSuccess = {
          // Do nothing; we just want to verify that the 'documents' field was accessed
        },
        onFailure = { fail("Failure callback should not be called") })

    // Verify that the 'documents' field was accessed
    verify(timeout(100)) { (mockProfileQuery).documents }
  }

  @Test
  fun getProfileById_onSuccess() {
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(mockDocumentSnapshot)
    `when`(mockDocumentReference.get()).thenReturn(task)
    `when`(mockDocumentSnapshot.exists()).thenReturn(true)
    `when`(mockDocumentSnapshot.id).thenReturn("1")
    `when`(mockDocumentSnapshot.getString("name")).thenReturn("John Doe")
    `when`(mockDocumentSnapshot.getString("email")).thenReturn("john.doe@gmail.com")
    `when`(mockDocumentSnapshot.getString("hikingLevel")).thenReturn("INTERMEDIATE")
    `when`(mockDocumentSnapshot.getTimestamp("joinedDate")).thenReturn(Timestamp(1609459200, 0))

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.getProfileById(
        "1",
        onSuccess = { p -> assert(p == profile) },
        onFailure = { fail("Failure callback should not be called") })

    verify(mockDocumentReference).get()
  }

  @Test
  fun getProfileById_onFailure() {
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(mockDocumentSnapshot)
    `when`(mockDocumentReference.get()).thenReturn(task)
    `when`(mockDocumentSnapshot.exists()).thenReturn(true)
    `when`(mockDocumentSnapshot.id).thenReturn("1")
    `when`(mockDocumentSnapshot.getString("name")).thenReturn("John Doe")
    `when`(mockDocumentSnapshot.getString("email")).thenThrow(RuntimeException("No email field"))
    `when`(mockDocumentSnapshot.getString("hikingLevel")).thenReturn("INTERMEDIATE")
    `when`(mockDocumentSnapshot.getTimestamp("joinedDate")).thenReturn(Timestamp(1609459200, 0))

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.getProfileById(
        "1",
        onSuccess = { assertNull(it) },
        onFailure = {
          // Do nothing; we just want to verify that this is called
        })

    verify(mockDocumentReference).get()
  }

  @Test
  fun getProfileById_onTaskFailed() {
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(task.isSuccessful).thenReturn(false)
    `when`(task.exception).thenReturn(RuntimeException("Failed to get profile"))
    `when`(mockDocumentReference.get()).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.getProfileById(
        "1",
        onSuccess = { fail("Success callback should not be called") },
        onFailure = {
          // Do nothing; we just want to verify that this is called
        })

    verify(timeout(100)) { mockDocumentReference.get() }
  }

  @Test
  fun addProfile_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null)) // Simulate success
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(mockDocumentSnapshot)
    `when`(mockDocumentReference.get()).thenReturn(task)

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.addProfile(profile, onSuccess = {}, onFailure = {})

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun addProfile_onSuccess() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(null)
    `when`(mockDocumentReference.set(any())).thenReturn(task)

    val getTask = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(getTask.isSuccessful).thenReturn(false) // Simulate that the profile does not exist yet
    `when`(getTask.exception).thenReturn(RuntimeException("Profile not found"))
    `when`(mockDocumentReference.get()).thenReturn(getTask)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.addProfile(
        profile,
        onSuccess = {
          // Do nothing; we just want to verify that the 'set' method was called
        },
        onFailure = { fail("Failure callback should not be called") })

    verify(timeout(100)) { mockDocumentReference.set(any()) }
  }

  @Test
  fun addProfile_onFailure() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(false)
    `when`(task.exception).thenReturn(RuntimeException("Failed to add profile"))
    `when`(mockDocumentReference.set(any())).thenReturn(task)

    val addTask = mock(Task::class.java) as Task<DocumentSnapshot>
    `when`(addTask.isSuccessful).thenReturn(true)
    `when`(addTask.result).thenReturn(mockDocumentSnapshot)
    `when`(mockDocumentReference.get()).thenReturn(addTask)

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(addTask)
          null
        }
        .`when`(addTask)
        .addOnCompleteListener(any())

    repository.addProfile(
        profile,
        onSuccess = { fail("Success callback should not be called") },
        onFailure = {
          // Do nothing; we just want to verify that the 'set' method was called
        })

    verify(timeout(100)) { mockDocumentReference.set(any()) }
  }

  @Test
  fun updateProfile_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null)) // Simulate success

    repository.updateProfile(profile, onSuccess = {}, onFailure = {})

    verify(mockDocumentReference).set(any())
  }

  @Test
  fun updateProfile_onSuccess() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(null)
    `when`(mockDocumentReference.set(any())).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.updateProfile(
        profile,
        onSuccess = {
          // Do nothing; we just want to verify that the 'set' method was called
        },
        onFailure = { fail("Failure callback should not be called") })

    verify(timeout(100)) { mockDocumentReference.set(any()) }
  }

  @Test
  fun updateProfile_onFailure() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(false)
    `when`(task.exception).thenReturn(RuntimeException("Failed to update profile"))
    `when`(mockDocumentReference.set(any())).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.updateProfile(
        profile,
        onSuccess = { fail("Success callback should not be called") },
        onFailure = {
          // Do nothing; we just want to verify that the 'set' method was called
        })

    verify(timeout(100)) { mockDocumentReference.set(any()) }
  }

  @Test
  fun deleteProfileById_shouldCallFirestoreCollection() {
    `when`(mockDocumentReference.delete()).thenReturn(Tasks.forResult(null)) // Simulate success

    repository.deleteProfileById("1", onSuccess = {}, onFailure = {})

    verify(mockDocumentReference).delete()
  }

  @Test
  fun deleteProfileById_onSuccess() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(true)
    `when`(task.result).thenReturn(null)
    `when`(mockDocumentReference.delete()).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.deleteProfileById(
        "1",
        onSuccess = {
          // Do nothing; we just want to verify that the 'delete' method was called
        },
        onFailure = { fail("Failure callback should not be called") })

    verify(timeout(100)) { mockDocumentReference.delete() }
  }

  @Test
  fun deleteProfileById_onFailure() {
    val task = mock(Task::class.java) as Task<Void>
    `when`(task.isSuccessful).thenReturn(false)
    `when`(task.exception).thenReturn(RuntimeException("Failed to delete profile"))
    `when`(mockDocumentReference.delete()).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<Void>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    repository.deleteProfileById(
        "1",
        onSuccess = { fail("Success callback should not be called") },
        onFailure = {
          // Do nothing; we just want to verify that the 'delete' method was called
        })

    verify(timeout(100)) { mockDocumentReference.delete() }
  }

  @Test
  fun addProfile_onProfileExistsCheckFailure() {
    // Arrange
    val task = mock(Task::class.java) as Task<DocumentSnapshot>
    val exception = Exception("")
    `when`(task.isSuccessful).thenReturn(false)
    `when`(task.exception).thenReturn(exception)
    `when`(mockDocumentReference.get()).thenReturn(task)

    // Simulate the task being completed
    doAnswer { invocation ->
          val listener = invocation.arguments[0] as OnCompleteListener<DocumentSnapshot>
          listener.onComplete(task)
          null
        }
        .`when`(task)
        .addOnCompleteListener(any())

    assertThrows(Exception::class.java) {
      repository.addProfile(profile, onSuccess = {}, onFailure = { throw it })
    }

    verify(mockDocumentReference).get()
  }
}
