import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class FetchFirestoreName : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    init {
        this.fetchUserData()
    }

    open fun fetchUserData() {
        val userId = auth.currentUser?.uid // Pobiera UID zalogowanego użytkownika
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstname=document.getString("firstname") ?: "Unknown"
                        val secondname=document.getString("secondname")?:"Unknown"
                        _userName.value ="$firstname $secondname"
                    } else {
                        _userName.value = "No Data"
                    }
                }
                .addOnFailureListener {
                    _userName.value = "Error"
                }
        } else {
            _userName.value = "Not Logged In"
        }
    }
}
