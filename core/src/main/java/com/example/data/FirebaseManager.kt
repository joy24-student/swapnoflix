package com.example.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.core.BuildConfig

enum class FirebaseStatus {
    UNCONFIGURED,    // Missing API keys / details in Secrets pane
    INITIALIZED,     // Successfully initialized programmatically
    ERROR            // Encountered error during setup
}

data class FirebaseUserState(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isGoogleUser: Boolean = false
)

data class SocialActiveUser(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val provider: String = "Google", // "Google", "Facebook", "Apple"
    val loginTime: String = "",
    val isOnline: Boolean = true,
    val avatarColorHex: String = "#E50914"
)

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    private val _status = MutableStateFlow(FirebaseStatus.UNCONFIGURED)
    val status = _status.asStateFlow()

    private val _statusDetail = MutableStateFlow("Firebase not initialized yet.")
    val statusDetail = _statusDetail.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUserState?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _activeSocialUsers = MutableStateFlow<List<SocialActiveUser>>(emptyList())
    val activeSocialUsers = _activeSocialUsers.asStateFlow()

    private val _fcmNotifications = kotlinx.coroutines.flow.MutableSharedFlow<NotificationItem>(extraBufferCapacity = 64)
    val fcmNotifications = _fcmNotifications.asSharedFlow()

    private val _lastFcmToken = MutableStateFlow("")
    val lastFcmToken = _lastFcmToken.asStateFlow()

    fun postFcmNotification(title: String, body: String, type: String = "REMINDER") {
        val item = NotificationItem(
            id = "FCM-${java.util.UUID.randomUUID().toString().take(6).uppercase()}",
            title = title,
            description = body,
            type = type,
            timestamp = "Just Now"
        )
        _fcmNotifications.tryEmit(item)
    }

    fun saveFcmToken(token: String) {
        _lastFcmToken.value = token
        Log.i(TAG, "FCM token updated in manager: $token")
        val currentUid = auth?.currentUser?.uid
        if (isRealFirebaseActive && currentUid != null && firestore != null) {
            val tokenData = mapOf(
                "token" to token,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore?.collection("users")?.document(currentUid)?.collection("fcmTokens")?.document(token)?.set(tokenData)
                ?.addOnSuccessListener { Log.d(TAG, "FCM Token successfully stored in Firestore") }
                ?.addOnFailureListener { e -> Log.w(TAG, "Error saving FCM token", e) }
        }
    }

    data class AppUpdateInfo(
        val version: String,
        val description: String,
        val downloadUrl: String,
        val forceUpdate: Boolean = false
    )

    private val _latestUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val latestUpdate = _latestUpdate.asStateFlow()

    fun dismissUpdate() {
        _latestUpdate.value = null
    }

    fun triggerSimulatedUpdate(version: String, description: String, downloadUrl: String, forceUpdate: Boolean = false) {
        _latestUpdate.value = AppUpdateInfo(version, description, downloadUrl, forceUpdate)
    }

    fun checkForUpdates() {
        if (!isRealFirebaseActive || firestore == null) {
            // Offline simulation of update check:
            // Set a default demo update so that the user can experience the popup immediately on startup!
            _latestUpdate.value = AppUpdateInfo(
                version = "2.0.1",
                description = "SwapnoFlix v2.0 is live! Included super-fast Cloudflare R2 object storage integration, background FCM pushes, offline video player optimizations, and modern Material 3 home pop-up dashboards.",
                downloadUrl = "https://cdn.swapnoflix.tv/downloads/swapnoflix-v2.apk",
                forceUpdate = false
            )
            return
        }
        firestore?.collection("appUpdates")?.document("latest")?.get()
            ?.addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val ver = doc.getString("version") ?: ""
                    val desc = doc.getString("description") ?: ""
                    val url = doc.getString("downloadUrl") ?: ""
                    val force = doc.getBoolean("forceUpdate") ?: false
                    
                    if (ver.isNotEmpty() && ver != "1.0") {
                        _latestUpdate.value = AppUpdateInfo(ver, desc, url, force)
                        Log.i(TAG, "Newer update detected from Firestore: $ver")
                    }
                }
            }
            ?.addOnFailureListener { e ->
                Log.w(TAG, "Failed to fetch updates from Firestore", e)
            }
    }

    private var isRealFirebaseActive = false

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var database: com.google.firebase.database.FirebaseDatabase? = null

    fun initialize(context: Context) {
        try {
            var appInitialized = false
            var currentProjectId = ""

            // 1. Check if Firebase auto-initialized via the google-services plugin / google-services.json
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val app = FirebaseApp.getInstance()
                currentProjectId = app.options.projectId ?: ""
                appInitialized = true
                Log.i(TAG, "Firebase auto-initialized via google-services.json for project: $currentProjectId")
            } else {
                // 2. Fallback: Initialize programmatically via BuildConfig secrets if available
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val appId = BuildConfig.FIREBASE_APP_ID
                val projectId = BuildConfig.FIREBASE_PROJECT_ID

                if (apiKey.isNotBlank() && appId.isNotBlank() && projectId.isNotBlank()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    currentProjectId = projectId
                    appInitialized = true
                    Log.i(TAG, "Firebase programmatically initialized with build secrets for project: $currentProjectId")
                }
            }

            if (!appInitialized) {
                isRealFirebaseActive = false
                _status.value = FirebaseStatus.UNCONFIGURED
                _statusDetail.value = "Missing keys/config: Ensure google-services.json is present, or configure FIREBASE_API_KEY in Secrets."
                Log.w(TAG, "Firebase options are incomplete. Active in offline fallback mode.")
                checkForUpdates()
                return
            }

            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            
            try {
                database = com.google.firebase.database.FirebaseDatabase.getInstance()
                Log.i(TAG, "Firebase Realtime Database initialized successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize Realtime Database: ${e.message}")
            }

            isRealFirebaseActive = true
            _status.value = FirebaseStatus.INITIALIZED
            _statusDetail.value = "Connected to Firebase project: $currentProjectId"

            // Run structural catalog initializer
            firestore?.let { db ->
                FirestoreDAL.initializeCollections(db)
            }

            auth?.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    _currentUser.value = FirebaseUserState(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: "Streamer",
                        isGoogleUser = fbUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    )
                } else {
                    _currentUser.value = null
                }
            }

            // Start listening to active social sessions
            startListeningToActiveSocialUsers()

            checkForUpdates()

        } catch (e: Exception) {
            isRealFirebaseActive = false
            _status.value = FirebaseStatus.ERROR
            _statusDetail.value = "Initialization error: ${e.message}"
            Log.e(TAG, "Fatal Firebase initialize exception: " + e.message, e)
        }
    }

    fun isRealFirebaseEnabled(): Boolean = isRealFirebaseActive

    fun getFirestore(): FirebaseFirestore? = firestore

    fun getDatabase(): com.google.firebase.database.FirebaseDatabase? = database

    // Firebase Authentication: Email & Password
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: (FirebaseUserState) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            // Fallback mock check
            if (email.contains("@") && password.length >= 4) {
                val mockUser = FirebaseUserState("mock_uid_123", email, "Guest Streamer", false)
                _currentUser.value = mockUser
                onSuccess(mockUser)
            } else {
                onFailure("Offline Fallback Mode: Please enter a valid email and >= 4 char password.")
            }
            return
        }

        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val userState = FirebaseUserState(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: "Streamer",
                        isGoogleUser = false
                    )
                    _currentUser.value = userState
                    onSuccess(userState)
                } else {
                    onFailure("Firebase sign in succeeded but user is null.")
                }
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Firebase Sign In Failed.")
            }
    }

    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        onSuccess: (FirebaseUserState) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            if (email.contains("@") && password.length >= 4) {
                val mockUser = FirebaseUserState("mock_uid_123", email, displayName, false)
                _currentUser.value = mockUser
                onSuccess(mockUser)
            } else {
                onFailure("Offline Fallback Mode: Valid email & >= 4 characters are required.")
            }
            return
        }

        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val userState = FirebaseUserState(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = displayName.ifBlank { "Streamer" },
                        isGoogleUser = false
                    )
                    _currentUser.value = userState
                    onSuccess(userState)
                } else {
                    onFailure("Registration succeeded but user details empty.")
                }
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Registration Failed.")
            }
    }

    // Google Sign-In with Firebase Auth Integration
    fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (FirebaseUserState) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            onFailure("Firebase SDK not connected. Configure Secrets for real Google Sign-In flow.")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnSuccessListener { result ->
                val fbUser = result.user
                if (fbUser != null) {
                    val userState = FirebaseUserState(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: "Google Streamer",
                        isGoogleUser = true
                    )
                    _currentUser.value = userState
                    onSuccess(userState)
                } else {
                    onFailure("Google Auth credential succeeded but user is null.")
                }
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Google Sign-In Credential Sync Failed.")
            }
    }

    private var activeSessionsListener: com.google.firebase.database.ValueEventListener? = null

    fun startListeningToActiveSocialUsers() {
        if (!isRealFirebaseActive || database == null) {
            // Provide nice mock active social users for fallback mode!
            _activeSocialUsers.value = listOf(
                SocialActiveUser("mock_usr1", "alex@gmail.com", "Alex (Adult)", "Google", "10:15 AM", true, "#E50914"),
                SocialActiveUser("mock_usr2", "sarah@facebook.com", "Sarah (Facebook)", "Facebook", "09:30 AM", true, "#2196F3"),
                SocialActiveUser("mock_usr3", "ryan@apple.com", "Ryan (Apple)", "Apple ID", "11:02 AM", true, "#4CAF50"),
                SocialActiveUser("mock_usr4", "priya@gmail.com", "Priya (Google)", "Google", "08:14 AM", true, "#9C27B0")
            )
            return
        }

        val ref = database?.getReference("active_sessions") ?: return
        activeSessionsListener?.let { ref.removeEventListener(it) }

        activeSessionsListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<SocialActiveUser>()
                for (child in snapshot.children) {
                    try {
                        val uid = child.key ?: ""
                        val email = child.child("email").value as? String ?: ""
                        val name = child.child("name").value as? String ?: ""
                        val provider = child.child("provider").value as? String ?: "Google"
                        val loginTime = child.child("loginTime").value as? String ?: ""
                        val isOnline = child.child("isOnline").value as? Boolean ?: true
                        val avatarColorHex = child.child("avatarColorHex").value as? String ?: "#E50914"

                        if (isOnline) {
                            list.add(
                                SocialActiveUser(
                                    uid = uid,
                                    email = email,
                                    name = name,
                                    provider = provider,
                                    loginTime = loginTime,
                                    isOnline = isOnline,
                                    avatarColorHex = avatarColorHex
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing active social user: ${e.message}")
                    }
                }
                _activeSocialUsers.value = list
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.w(TAG, "Active social users listener cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(activeSessionsListener!!)
    }

    fun registerActiveSocialSession(provider: String, displayName: String = "") {
        val user = _currentUser.value ?: return
        val loginTimeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        
        val colors = listOf("#E50914", "#00FFC4", "#2196F3", "#9C27B0", "#FFC107", "#4CAF50")
        val colorHex = colors[Math.abs(user.email.hashCode()) % colors.size]

        val sessionUser = SocialActiveUser(
            uid = user.uid,
            email = user.email,
            name = displayName.ifBlank { user.displayName.ifBlank { user.email.substringBefore("@") } },
            provider = provider,
            loginTime = loginTimeStr,
            isOnline = true,
            avatarColorHex = colorHex
        )

        if (!isRealFirebaseActive || database == null) {
            val current = _activeSocialUsers.value.toMutableList()
            current.removeAll { it.uid == sessionUser.uid }
            current.add(0, sessionUser)
            _activeSocialUsers.value = current
            return
        }

        val ref = database?.getReference("active_sessions")?.child(user.uid)
        ref?.setValue(sessionUser)
            ?.addOnSuccessListener {
                Log.i(TAG, "Active social session registered: ${sessionUser.name}")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to register social session: ${e.message}")
            }
    }

    fun clearActiveSocialSession() {
        val user = _currentUser.value ?: return
        if (!isRealFirebaseActive || database == null) {
            val current = _activeSocialUsers.value.toMutableList()
            current.removeAll { it.uid == user.uid }
            _activeSocialUsers.value = current
            return
        }

        val ref = database?.getReference("active_sessions")?.child(user.uid)
        ref?.removeValue()
    }

    fun logOutFromFirebase() {
        clearActiveSocialSession()
        if (isRealFirebaseActive && auth != null) {
            auth?.signOut()
        }
        _currentUser.value = null
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            onSuccess()
            return
        }
        auth?.sendPasswordResetEmail(email)
            ?.addOnSuccessListener {
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to send reset email.")
            }
    }

    fun updateUserPassword(
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            onSuccess()
            return
        }
        auth?.currentUser?.updatePassword(newPassword)
            ?.addOnSuccessListener {
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to update password. Re-authentication may be required.")
            }
    }

    fun updateUserEmail(
        newEmail: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            val current = _currentUser.value
            if (current != null) {
                val updated = current.copy(email = newEmail)
                _currentUser.value = updated
            }
            onSuccess()
            return
        }
        auth?.currentUser?.updateEmail(newEmail)
            ?.addOnSuccessListener {
                val current = _currentUser.value
                if (current != null) {
                    _currentUser.value = current.copy(email = newEmail)
                }
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to update email. Re-authentication may be required.")
            }
    }

    fun updateUserDisplayName(
        newDisplayName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isRealFirebaseActive || auth == null) {
            val current = _currentUser.value
            if (current != null) {
                val updated = current.copy(displayName = newDisplayName)
                _currentUser.value = updated
            }
            onSuccess()
            return
        }
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(newDisplayName)
            .build()
        auth?.currentUser?.updateProfile(profileUpdates)
            ?.addOnSuccessListener {
                val current = _currentUser.value
                if (current != null) {
                    _currentUser.value = current.copy(displayName = newDisplayName)
                }
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to update display name.")
            }
    }

    fun deleteUserAccount(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        clearActiveSocialSession()
        if (!isRealFirebaseActive || auth == null) {
            _currentUser.value = null
            onSuccess()
            return
        }
        val user = auth?.currentUser
        user?.delete()
            ?.addOnSuccessListener {
                _currentUser.value = null
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to delete account. Re-authentication may be required.")
            }
    }

    // Firestore Integration: Real User Profiles, Favorites, Support Tickets, and Subscription sync
    fun saveUserDataToFirestore(
        profiles: List<UserProfile>,
        favoriteChannels: Set<String>,
        favoriteMovies: Set<String>,
        favoriteSeries: Set<String>,
        activePlanName: String,
        invoices: List<InvoiceRecord>,
        tickets: List<SupportTicket>,
        rewardPoints: Int,
        referralEarnings: Int,
        referralCode: String,
        parentalPin: String,
        isParentalLockActive: Boolean,
        allowedAgeBracket: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        if (!isRealFirebaseActive || firestore == null) {
            onComplete(true) // Treat as success in fallback
            return
        }

        val userDocRef = firestore?.collection("users")?.document(user.uid) ?: return

        // Format data map for Firestore document
        val dataMap = hashMapOf(
            "email" to user.email,
            "displayName" to user.displayName,
            "activePlanName" to activePlanName,
            "rewardPoints" to rewardPoints,
            "referralEarnings" to referralEarnings,
            "referralCode" to referralCode,
            "parentalPin" to parentalPin,
            "isParentalLockActive" to isParentalLockActive,
            "allowedAgeBracket" to allowedAgeBracket,
            "profiles" to profiles.map { profile ->
                hashMapOf(
                    "id" to profile.id,
                    "name" to profile.name,
                    "isKids" to profile.isKids,
                    "ageCategory" to profile.ageCategory,
                    "contentPreference" to profile.contentPreference,
                    "colorHex" to String.format("#%06X", 0xFFFFFF and profile.iconColor.value.toLong().toInt())
                )
            },
            "favoriteChannels" to favoriteChannels.toList(),
            "favoriteMovies" to favoriteMovies.toList(),
            "favoriteSeries" to favoriteSeries.toList(),
            "invoices" to invoices.map { inv ->
                hashMapOf(
                    "id" to inv.id,
                    "date" to inv.date,
                    "planName" to inv.planName,
                    "amount" to inv.amount,
                    "paymentMethod" to inv.paymentMethod,
                    "status" to inv.status
                )
            },
            "supportTickets" to tickets.map { ticket ->
                hashMapOf(
                    "id" to ticket.id,
                    "subject" to ticket.subject,
                    "category" to ticket.category,
                    "description" to ticket.description,
                    "date" to ticket.date,
                    "status" to ticket.status
                )
            }
        )

        userDocRef.set(dataMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User state saved to Firestore successfully!")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving state to Firestore: ${e.message}", e)
                onComplete(false)
            }
    }

    fun loadUserDataFromFirestore(
        onLoaded: (
            profiles: List<UserProfile>?,
            favoriteChannels: Set<String>?,
            favoriteMovies: Set<String>?,
            favoriteSeries: Set<String>?,
            activePlanName: String?,
            invoices: List<InvoiceRecord>?,
            tickets: List<SupportTicket>?,
            rewardPoints: Int?,
            referralEarnings: Int?,
            referralCode: String?,
            parentalPin: String?,
            isParentalLockActive: Boolean?,
            allowedAgeBracket: String?
        ) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (!isRealFirebaseActive || firestore == null) {
            onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
            return
        }

        val userDocRef = firestore?.collection("users")?.document(user.uid) ?: return
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val activePlanName = document.getString("activePlanName")
                        val rewardPoints = document.getLong("rewardPoints")?.toInt()
                        val referralEarnings = document.getLong("referralEarnings")?.toInt()
                        val referralCode = document.getString("referralCode")
                        val parentalPin = document.getString("parentalPin")
                        val isParentalLockActive = document.getBoolean("isParentalLockActive")
                        val allowedAgeBracket = document.getString("allowedAgeBracket")
                        
                        val profilesList = (document.get("profiles") as? List<Map<String, Any>>)?.map { p ->
                            val colorHex = p["colorHex"] as? String ?: "#E50914"
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(ex: Exception) { Color(0xFFE50914) }
                            UserProfile(
                                id = p["id"] as? String ?: "",
                                name = p["name"] as? String ?: "Streamer",
                                iconColor = color,
                                isKids = p["isKids"] as? Boolean ?: false,
                                ageCategory = p["ageCategory"] as? String ?: "18+",
                                contentPreference = p["contentPreference"] as? String ?: "Everything"
                            )
                        }

                        val favChannelsList = (document.get("favoriteChannels") as? List<*>)?.mapNotNull { it?.toString() }?.toSet()
                        val favMoviesList = (document.get("favoriteMovies") as? List<*>)?.mapNotNull { it?.toString() }?.toSet()
                        val favSeriesList = (document.get("favoriteSeries") as? List<*>)?.mapNotNull { it?.toString() }?.toSet()

                        val invoicesList = (document.get("invoices") as? List<Map<String, Any>>)?.map { inv ->
                            InvoiceRecord(
                                id = inv["id"] as? String ?: "",
                                date = inv["date"] as? String ?: "",
                                planName = inv["planName"] as? String ?: "SwapnoFlix Premium",
                                amount = inv["amount"] as? String ?: "৳499.00",
                                paymentMethod = inv["paymentMethod"] as? String ?: "Credit Card",
                                status = inv["status"] as? String ?: "PAID"
                            )
                        }

                        val supportTicketsList = (document.get("supportTickets") as? List<Map<String, Any>>)?.map { ticket ->
                            SupportTicket(
                                id = ticket["id"] as? String ?: "",
                                subject = ticket["subject"] as? String ?: "",
                                category = ticket["category"] as? String ?: "Billing",
                                description = ticket["description"] as? String ?: "",
                                date = ticket["date"] as? String ?: "",
                                status = ticket["status"] as? String ?: "OPEN"
                            )
                        }

                        onLoaded(
                            profilesList,
                            favChannelsList,
                            favMoviesList,
                            favSeriesList,
                            activePlanName,
                            invoicesList,
                            supportTicketsList,
                            rewardPoints,
                            referralEarnings,
                            referralCode,
                            parentalPin,
                            isParentalLockActive,
                            allowedAgeBracket
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Parsing elements failed from Firestore load schema: ${e.message}", e)
                        onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
                    }
                } else {
                    onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching developer state document: ${e.message}", e)
                onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
            }
    }

    fun saveUserDataToRealtimeDatabase(
        profiles: List<UserProfile>,
        favoriteChannels: Set<String>,
        favoriteMovies: Set<String>,
        favoriteSeries: Set<String>,
        activePlanName: String,
        invoices: List<InvoiceRecord>,
        tickets: List<SupportTicket>,
        rewardPoints: Int,
        referralEarnings: Int,
        referralCode: String,
        parentalPin: String,
        isParentalLockActive: Boolean,
        allowedAgeBracket: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        if (!isRealFirebaseActive || database == null) {
            onComplete(true) // Treat as success in fallback
            return
        }

        val userRef = database?.getReference("users")?.child(user.uid) ?: return

        // Format data map for Realtime Database
        val dataMap = hashMapOf(
            "email" to user.email,
            "displayName" to user.displayName,
            "activePlanName" to activePlanName,
            "rewardPoints" to rewardPoints,
            "referralEarnings" to referralEarnings,
            "referralCode" to referralCode,
            "parentalPin" to parentalPin,
            "isParentalLockActive" to isParentalLockActive,
            "allowedAgeBracket" to allowedAgeBracket,
            "profiles" to profiles.map { profile ->
                hashMapOf(
                    "id" to profile.id,
                    "name" to profile.name,
                    "isKids" to profile.isKids,
                    "ageCategory" to profile.ageCategory,
                    "contentPreference" to profile.contentPreference,
                    "colorHex" to String.format("#%06X", 0xFFFFFF and profile.iconColor.value.toLong().toInt())
                )
            },
            "favoriteChannels" to favoriteChannels.toList(),
            "favoriteMovies" to favoriteMovies.toList(),
            "favoriteSeries" to favoriteSeries.toList(),
            "invoices" to invoices.map { inv ->
                hashMapOf(
                    "id" to inv.id,
                    "date" to inv.date,
                    "planName" to inv.planName,
                    "amount" to inv.amount,
                    "paymentMethod" to inv.paymentMethod,
                    "status" to inv.status
                )
            },
            "supportTickets" to tickets.map { ticket ->
                hashMapOf(
                    "id" to ticket.id,
                    "subject" to ticket.subject,
                    "category" to ticket.category,
                    "description" to ticket.description,
                    "date" to ticket.date,
                    "status" to ticket.status
                )
            }
        )

        userRef.setValue(dataMap)
            .addOnSuccessListener {
                Log.d(TAG, "User state saved to Realtime Database successfully!")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving state to Realtime Database: ${e.message}", e)
                onComplete(false)
            }
    }

    fun loadUserDataFromRealtimeDatabase(
        onLoaded: (
            profiles: List<UserProfile>?,
            favoriteChannels: Set<String>?,
            favoriteMovies: Set<String>?,
            favoriteSeries: Set<String>?,
            activePlanName: String?,
            invoices: List<InvoiceRecord>?,
            tickets: List<SupportTicket>?,
            rewardPoints: Int?,
            referralEarnings: Int?,
            referralCode: String?,
            parentalPin: String?,
            isParentalLockActive: Boolean?,
            allowedAgeBracket: String?
        ) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (!isRealFirebaseActive || database == null) {
            onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
            return
        }

        val userRef = database?.getReference("users")?.child(user.uid) ?: return
        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        val activePlanName = snapshot.child("activePlanName").value as? String
                        val rewardPoints = (snapshot.child("rewardPoints").value as? Long)?.toInt()
                        val referralEarnings = (snapshot.child("referralEarnings").value as? Long)?.toInt()
                        val referralCode = snapshot.child("referralCode").value as? String
                        val parentalPin = snapshot.child("parentalPin").value as? String
                        val isParentalLockActive = snapshot.child("isParentalLockActive").value as? Boolean
                        val allowedAgeBracket = snapshot.child("allowedAgeBracket").value as? String
                        
                        val profilesList = (snapshot.child("profiles").value as? List<Map<String, Any>>)?.map { p ->
                            val colorHex = p["colorHex"] as? String ?: "#E50914"
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(ex: Exception) { Color(0xFFE50914) }
                            UserProfile(
                                id = p["id"] as? String ?: "",
                                name = p["name"] as? String ?: "Streamer",
                                iconColor = color,
                                isKids = p["isKids"] as? Boolean ?: false,
                                ageCategory = p["ageCategory"] as? String ?: "18+",
                                contentPreference = p["contentPreference"] as? String ?: "Everything"
                            )
                        }

                        val favChannelsList = (snapshot.child("favoriteChannels").value as? List<*>)?.mapNotNull { it?.toString() }?.toSet()
                        val favMoviesList = (snapshot.child("favoriteMovies").value as? List<*>)?.mapNotNull { it?.toString() }?.toSet()
                        val favSeriesList = (snapshot.child("favoriteSeries").value as? List<*>)?.mapNotNull { it?.toString() }?.toSet()

                        val invoicesList = (snapshot.child("invoices").value as? List<Map<String, Any>>)?.map { inv ->
                            InvoiceRecord(
                                id = inv["id"] as? String ?: "",
                                date = inv["date"] as? String ?: "",
                                planName = inv["planName"] as? String ?: "SwapnoFlix Premium",
                                amount = inv["amount"] as? String ?: "৳499.00",
                                paymentMethod = inv["paymentMethod"] as? String ?: "Credit Card",
                                status = inv["status"] as? String ?: "PAID"
                            )
                        }

                        val supportTicketsList = (snapshot.child("supportTickets").value as? List<Map<String, Any>>)?.map { ticket ->
                            SupportTicket(
                                id = ticket["id"] as? String ?: "",
                                subject = ticket["subject"] as? String ?: "",
                                category = ticket["category"] as? String ?: "Billing",
                                description = ticket["description"] as? String ?: "",
                                date = ticket["date"] as? String ?: "",
                                status = ticket["status"] as? String ?: "OPEN"
                            )
                        }

                        onLoaded(
                            profilesList,
                            favChannelsList,
                            favMoviesList,
                            favSeriesList,
                            activePlanName,
                            invoicesList,
                            supportTicketsList,
                            rewardPoints,
                            referralEarnings,
                            referralCode,
                            parentalPin,
                            isParentalLockActive,
                            allowedAgeBracket
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Parsing elements failed from Realtime Database load schema: ${e.message}", e)
                        onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
                    }
                } else {
                    onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user state from Realtime Database: ${e.message}", e)
                onLoaded(null, null, null, null, null, null, null, null, null, null, null, null, null)
            }
    }
}
