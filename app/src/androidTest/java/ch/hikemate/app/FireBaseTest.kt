package ch.hikemate.app

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FireBaseTest {

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FirebaseApp.initializeApp(context)
  }

  @Test
  fun testFirebaseReadAndWrite() {
    val db = FirebaseFirestore.getInstance()

    val latchAdd = CountDownLatch(1)
    val latchGet = CountDownLatch(1)
    val latchDel = CountDownLatch(1)

    val testData: MutableMap<String, Any> = HashMap()
    val testValue = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    testData["testKey"] = testValue

    // Add a test file to the database
    db.collection("testCollection")
        .add(testData)
        .addOnSuccessListener { documentReference ->
          assertTrue(true)
          Log.d("FireBaseTest", "DocumentSnapshot added with ID: " + documentReference.id)
          latchAdd.countDown()
        }
        .addOnFailureListener { e ->
          assertTrue("Write to Firebase failed. Got error: $e", false)
          latchAdd.countDown()
        }

    assertTrue(latchAdd.await(5, TimeUnit.SECONDS))

    // Read the test file from the database
    db.collection("testCollection").get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        assertTrue(containsAddedValue(task.result, "testKey", testValue))
      } else {
        assertTrue("Read from Firebase failed. Got error: " + "${task.exception}", false)
      }
      latchGet.countDown()
    }

    assertTrue(latchGet.await(5, TimeUnit.SECONDS))

    // Delete the test file from the database
    db.collection("testCollection").get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        for (document in task.result!!) {
          db.collection("testCollection")
              .document(document.id)
              .delete()
              .addOnSuccessListener {
                assertTrue("Delete from Firebase worked. Deleted document: " + document.id, true)
                Log.d("FireBaseTest", "DocumentSnapshot successfully deleted!")
              }
              .addOnFailureListener { e ->
                assertTrue("Delete from Firebase failed. Got error: $e", false)
              }
        }
      } else {
        assertTrue("Read from Firebase failed. Got error: " + "${task.exception}", false)
      }

      latchDel.countDown()
    }

    assertTrue(latchDel.await(5, TimeUnit.SECONDS))
  }

  private fun containsAddedValue(snapshot: QuerySnapshot?, key: String, value: String): Boolean {
    // Return false if the snapshot is null or empty
    if (snapshot == null || snapshot.isEmpty) {
      return false
    }

    // Iterate through each document in the result
    for (document in snapshot.documents) {
      // Check if the document contains the specific key and value
      if (document.contains(key) && document.getString(key) == value) {
        return true // Value found
      }
    }
    return false // Value not found
  }
}
